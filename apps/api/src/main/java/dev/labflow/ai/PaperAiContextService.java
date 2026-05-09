package dev.labflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.labflow.file.FileContent;
import dev.labflow.file.FileService;
import dev.labflow.paper.Paper;
import dev.labflow.paper.PaperRepository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
public class PaperAiContextService {
    private final ObjectMapper objectMapper;
    private final PaperRepository paperRepository;
    private final FileService fileService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final PdfTextChunker pdfTextChunker;

    public PaperAiContextService(
            ObjectMapper objectMapper,
            PaperRepository paperRepository,
            FileService fileService,
            PdfTextExtractionService pdfTextExtractionService,
            PdfTextChunker pdfTextChunker
    ) {
        this.objectMapper = objectMapper;
        this.paperRepository = paperRepository;
        this.fileService = fileService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.pdfTextChunker = pdfTextChunker;
    }

    public String enrichPaperCardInput(AiJob job, JsonNode input) {
        ObjectNode payload = toObjectPayload(input);
        if (!isPaperCardJob(job)) {
            return payload.toString();
        }
        if (job.getTargetId() == null) {
            payload.put("pdfContextStatus", "missing-paper-target");
            payload.put("pdfContextWarning", "AI job targetId is empty, so LabFlow could not locate the paper PDF.");
            return payload.toString();
        }

        paperRepository.findById(job.getTargetId()).ifPresentOrElse(
                paper -> enrichFromPaper(payload, paper),
                () -> {
                    payload.put("pdfContextStatus", "paper-not-found");
                    payload.put("pdfContextWarning", "LabFlow could not find the target paper for PDF context extraction.");
                }
        );
        return payload.toString();
    }

    private ObjectNode toObjectPayload(JsonNode input) {
        if (input != null && input.isObject()) {
            return ((ObjectNode) input).deepCopy();
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("rawInput", input == null ? objectMapper.createObjectNode() : input);
        return payload;
    }

    private boolean isPaperCardJob(AiJob job) {
        return "paper-card".equalsIgnoreCase(job.getJobType())
                && "paper".equalsIgnoreCase(job.getTargetType());
    }

    private void enrichFromPaper(ObjectNode payload, Paper paper) {
        putIfMissing(payload, "paperId", paper.getId().toString());
        putIfMissing(payload, "title", nullToBlank(paper.getTitle()));
        putIfMissing(payload, "abstract", nullToBlank(paper.getKeyClaims()));
        putIfMissing(payload, "notes", nullToBlank(paper.getNotes()));
        payload.put("authors", nullToBlank(paper.getAuthors()));
        payload.put("publication", nullToBlank(paper.getPublication()));
        payload.put("publishedDate", nullToBlank(paper.getPublishedDate()));
        payload.put("tags", nullToBlank(paper.getTags()));
        payload.put("url", nullToBlank(paper.getUrl()));

        if (paper.getPdfFileId() == null) {
            payload.put("pdfContextStatus", "no-pdf-linked");
            payload.put("pdfContextWarning", "This paper has no linked PDF, so the card is based on metadata only.");
            return;
        }

        try {
            FileContent content = fileService.open(paper.getPdfFileId());
            try (InputStream stream = content.stream()) {
                String text = pdfTextExtractionService.extract(stream);
                List<String> snippets = pdfTextChunker.selectSnippets(text, paper.getTitle(), paper.getTags());
                ArrayNode sourceSnippets = payload.putArray("sourceSnippets");
                snippets.forEach(sourceSnippets::add);
                payload.put("pdfContextStatus", snippets.isEmpty() ? "pdf-empty-text" : "pdf-snippets-attached");
                payload.put("pdfFilename", nullToBlank(content.filename()));
                payload.put("pdfSnippetCount", snippets.size());
                if (snippets.isEmpty()) {
                    payload.put("pdfContextWarning", "LabFlow read the linked PDF but could not extract useful text.");
                }
            }
        } catch (RuntimeException ex) {
            payload.put("pdfContextStatus", "pdf-extraction-failed");
            payload.put("pdfContextWarning", cleanError(ex));
        } catch (Exception ex) {
            payload.put("pdfContextStatus", "pdf-extraction-failed");
            payload.put("pdfContextWarning", cleanError(ex));
        }
    }

    private static void putIfMissing(ObjectNode payload, String field, String value) {
        if (!payload.hasNonNull(field) || payload.get(field).asText().isBlank()) {
            payload.put(field, value);
        }
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static String cleanError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "LabFlow failed to extract text from the linked PDF.";
        }
        return message.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
