package dev.labflow.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.labflow.ai.AiJobRepository;
import dev.labflow.ai.AiJobStatus;
import dev.labflow.ai.AiServiceProperties;
import dev.labflow.ai.PdfTextExtractionService;
import dev.labflow.common.NotFoundException;
import dev.labflow.experiment.Experiment;
import dev.labflow.experiment.ExperimentRepository;
import dev.labflow.file.FileContent;
import dev.labflow.file.FileService;
import dev.labflow.paper.Paper;
import dev.labflow.paper.PaperRepository;
import dev.labflow.project.ProjectService;
import dev.labflow.rag.dto.RagDtos.RagAskResponse;
import dev.labflow.rag.dto.RagDtos.RagChunkResponse;
import dev.labflow.rag.dto.RagDtos.RagIndexResponse;
import dev.labflow.rag.dto.RagDtos.RagSearchResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RagService {
    private static final int SEARCH_LIMIT = 6;

    private final RagChunkRepository ragChunkRepository;
    private final ProjectService projectService;
    private final PaperRepository paperRepository;
    private final ExperimentRepository experimentRepository;
    private final AiJobRepository aiJobRepository;
    private final FileService fileService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final RagTextChunker ragTextChunker;
    private final LocalEmbeddingService embeddingService;
    private final AiServiceProperties aiServiceProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RagService(
            RagChunkRepository ragChunkRepository,
            ProjectService projectService,
            PaperRepository paperRepository,
            ExperimentRepository experimentRepository,
            AiJobRepository aiJobRepository,
            FileService fileService,
            PdfTextExtractionService pdfTextExtractionService,
            RagTextChunker ragTextChunker,
            LocalEmbeddingService embeddingService,
            AiServiceProperties aiServiceProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.ragChunkRepository = ragChunkRepository;
        this.projectService = projectService;
        this.paperRepository = paperRepository;
        this.experimentRepository = experimentRepository;
        this.aiJobRepository = aiJobRepository;
        this.fileService = fileService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.ragTextChunker = ragTextChunker;
        this.embeddingService = embeddingService;
        this.aiServiceProperties = aiServiceProperties;
        this.restClient = restClientBuilder.baseUrl(aiServiceProperties.baseUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RagIndexResponse indexProject(UUID projectId) {
        projectService.getEntity(projectId);
        ragChunkRepository.deleteByProjectId(projectId);
        List<RagChunk> chunks = buildProjectChunks(projectId);
        ragChunkRepository.saveAll(chunks);
        return new RagIndexResponse(projectId, chunks.size(), Instant.now());
    }

    @Transactional(readOnly = true)
    public List<RagChunkResponse> listChunks(UUID projectId) {
        projectService.getEntity(projectId);
        return ragChunkRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(chunk -> toResponse(chunk, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public RagSearchResponse search(UUID projectId, String question) {
        projectService.getEntity(projectId);
        List<RagChunkResponse> results = searchChunks(projectId, question);
        return new RagSearchResponse(projectId, question, results);
    }

    @Transactional(readOnly = true)
    public RagAskResponse ask(UUID projectId, String question) {
        projectService.getEntity(projectId);
        List<RagChunkResponse> contexts = searchChunks(projectId, question);
        String answerMarkdown = contexts.isEmpty()
                ? "项目知识库还没有可用片段。请先点击索引知识库，再上传论文 PDF 或补充论文/实验记录。"
                : generateAnswer(question, contexts);
        return new RagAskResponse(projectId, question, answerMarkdown, contexts);
    }

    private List<RagChunk> buildProjectChunks(UUID projectId) {
        List<RagChunk> paperChunks = paperRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .flatMap(paper -> buildPaperChunks(projectId, paper).stream())
                .toList();
        List<RagChunk> experimentChunks = experimentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .flatMap(experiment -> ragTextChunker.split(experimentText(experiment)).stream()
                        .map(text -> createChunk(projectId, "EXPERIMENT", experiment.getId(), experiment.getName(), text, "{}")))
                .toList();
        return java.util.stream.Stream.concat(paperChunks.stream(), experimentChunks.stream()).toList();
    }

    private List<RagChunk> buildPaperChunks(UUID projectId, Paper paper) {
        String metadataText = paperText(paper);
        String pdfText = extractPdfText(paper);
        String text = pdfText.isBlank() ? metadataText : metadataText + "\n\nPDF 正文：\n" + pdfText;
        String metadataJson = paperMetadataJson(paper, !pdfText.isBlank());
        return ragTextChunker.split(text).stream()
                .map(chunk -> createChunk(projectId, "PAPER", paper.getId(), paper.getTitle(), chunk, metadataJson))
                .toList();
    }

    private RagChunk createChunk(UUID projectId, String sourceType, UUID sourceId, String sourceTitle, String contentText, String metadataJson) {
        RagChunk chunk = new RagChunk();
        chunk.setProjectId(projectId);
        chunk.setSourceType(sourceType);
        chunk.setSourceId(sourceId);
        chunk.setSourceTitle(blankToFallback(sourceTitle, sourceType));
        chunk.setContentText(contentText);
        chunk.setMetadataJson(metadataJson);
        chunk.setEmbeddingModel(LocalEmbeddingService.MODEL_NAME);
        chunk.setEmbeddingJson(embeddingService.toJson(embeddingService.embed(sourceTitle + "\n" + contentText)));
        return chunk;
    }

    private List<RagChunkResponse> searchChunks(UUID projectId, String question) {
        double[] queryEmbedding = embeddingService.embed(question);
        return ragChunkRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(chunk -> toResponse(chunk, boostedScore(chunk, queryEmbedding, question)))
                .sorted(Comparator.comparingDouble(RagChunkResponse::score).reversed()
                        .thenComparing(response -> "PAPER".equals(response.sourceType()) ? 0 : 1))
                .limit(SEARCH_LIMIT)
                .toList();
    }

    private double boostedScore(RagChunk chunk, double[] queryEmbedding, String question) {
        double score = embeddingService.cosine(queryEmbedding, embeddingService.fromJson(chunk.getEmbeddingJson()));
        String questionLower = question == null ? "" : question.toLowerCase();
        String titleLower = chunk.getSourceTitle() == null ? "" : chunk.getSourceTitle().toLowerCase();
        if (!questionLower.isBlank() && titleLower.contains("nids") && questionLower.contains("nids")) {
            score += 0.08;
        }
        if ("PAPER".equalsIgnoreCase(chunk.getSourceType())) {
            score += 0.03;
        }
        return score;
    }

    private String generateAnswer(String question, List<RagChunkResponse> contexts) {
        if (aiServiceProperties.mockEnabled()) {
            return mockAnswer(question, contexts);
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("question", question);
        ArrayNode contextNodes = payload.putArray("contexts");
        contexts.forEach(context -> {
            ObjectNode node = contextNodes.addObject();
            node.put("sourceType", context.sourceType());
            node.put("sourceTitle", context.sourceTitle());
            node.put("contentText", context.contentText());
            node.put("score", context.score());
        });
        return restClient.post()
                .uri("/rag/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload.toString())
                .retrieve()
                .body(JsonNode.class)
                .path("answerMarkdown")
                .asText(mockAnswer(question, contexts));
    }

    private static String mockAnswer(String question, List<RagChunkResponse> contexts) {
        RagChunkResponse first = contexts.get(0);
        String excerpt = first.contentText().length() > 220 ? first.contentText().substring(0, 220).strip() + "..." : first.contentText();
        return """
                ## 回答

                根据当前项目知识库，问题「%s」最相关的资料来自 **%s**。核心线索是：%s

                ## 依据

                - [1] %s：%s
                """.formatted(question, first.sourceTitle(), excerpt, first.sourceTitle(), first.sourceType());
    }

    private String extractPdfText(Paper paper) {
        if (paper.getPdfFileId() == null) {
            return "";
        }
        try {
            FileContent content = fileService.open(paper.getPdfFileId());
            try (var stream = content.stream()) {
                return pdfTextExtractionService.extract(stream);
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private String paperText(Paper paper) {
        StringBuilder builder = new StringBuilder();
        append(builder, "标题", paper.getTitle());
        append(builder, "作者", paper.getAuthors());
        append(builder, "出版物", paper.getPublication());
        append(builder, "日期", paper.getPublishedDate());
        append(builder, "标签", paper.getTags());
        append(builder, "关键 claim", paper.getKeyClaims());
        append(builder, "笔记", paper.getNotes());
        aiJobRepository.findFirstByTargetTypeIgnoreCaseAndTargetIdAndJobTypeIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        "PAPER", paper.getId(), "paper-card", AiJobStatus.SUCCESS)
                .ifPresent(job -> append(builder, "AI 论文卡片", readableAiOutput(job.getOutputJson())));
        return builder.toString();
    }

    private static String experimentText(Experiment experiment) {
        StringBuilder builder = new StringBuilder();
        append(builder, "实验", experiment.getName());
        append(builder, "状态", experiment.getStatus() == null ? "" : experiment.getStatus().name());
        append(builder, "GitHub 仓库", experiment.getRepoUrl());
        append(builder, "Commit", experiment.getCommitHash());
        append(builder, "数据集", experiment.getDataset());
        append(builder, "配置", experiment.getConfig());
        append(builder, "指标", experiment.getMetrics());
        append(builder, "结论", experiment.getConclusion());
        append(builder, "失败原因", experiment.getFailureReason());
        append(builder, "下一步", experiment.getNextStep());
        return builder.toString();
    }

    private String readableAiOutput(String outputJson) {
        if (outputJson == null || outputJson.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(outputJson);
            StringBuilder builder = new StringBuilder();
            append(builder, "摘要", node.path("summary").asText(""));
            append(builder, "主要贡献", joinArray(node.path("contributions")));
            append(builder, "局限性", joinArray(node.path("limitations")));
            return builder.toString();
        } catch (JsonProcessingException ex) {
            return outputJson;
        }
    }

    private static String joinArray(JsonNode node) {
        if (!node.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        node.forEach(item -> {
            if (!item.asText("").isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append("；");
                }
                builder.append(item.asText());
            }
        });
        return builder.toString();
    }

    private String paperMetadataJson(Paper paper, boolean pdfIncluded) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("pdfIncluded", pdfIncluded);
        node.put("tags", nullToBlank(paper.getTags()));
        node.put("publication", nullToBlank(paper.getPublication()));
        return node.toString();
    }

    private static void append(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append("：").append(value.strip()).append('\n');
        }
    }

    private static String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static RagChunkResponse toResponse(RagChunk chunk, double score) {
        return new RagChunkResponse(
                chunk.getId(),
                chunk.getProjectId(),
                chunk.getSourceType(),
                chunk.getSourceId(),
                chunk.getSourceTitle(),
                chunk.getContentText(),
                score,
                chunk.getMetadataJson(),
                chunk.getCreatedAt(),
                chunk.getUpdatedAt()
        );
    }
}
