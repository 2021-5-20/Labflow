package dev.labflow.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.labflow.ai.AiJobRepository;
import dev.labflow.ai.AiJobStatus;
import dev.labflow.common.NotFoundException;
import dev.labflow.experiment.Experiment;
import dev.labflow.experiment.ExperimentRepository;
import dev.labflow.experiment.ExperimentStatus;
import dev.labflow.paper.Paper;
import dev.labflow.paper.PaperRepository;
import dev.labflow.project.Project;
import dev.labflow.project.ProjectService;
import dev.labflow.report.dto.WeeklyReportDtos.WeeklyReportRequest;
import dev.labflow.report.dto.WeeklyReportDtos.WeeklyReportPreviewResponse;
import dev.labflow.report.dto.WeeklyReportDtos.WeeklyReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.AbstractMap;
import java.util.stream.Collectors;

@Service
public class WeeklyReportService {
    private final WeeklyReportRepository weeklyReportRepository;
    private final ProjectService projectService;
    private final PaperRepository paperRepository;
    private final ExperimentRepository experimentRepository;
    private final AiJobRepository aiJobRepository;
    private final ObjectMapper objectMapper;

    public WeeklyReportService(
            WeeklyReportRepository weeklyReportRepository,
            ProjectService projectService,
            PaperRepository paperRepository,
            ExperimentRepository experimentRepository,
            AiJobRepository aiJobRepository,
            ObjectMapper objectMapper
    ) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.projectService = projectService;
        this.paperRepository = paperRepository;
        this.experimentRepository = experimentRepository;
        this.aiJobRepository = aiJobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WeeklyReportResponse generate(UUID projectId, WeeklyReportRequest request) {
        validateDateRange(request);
        WeeklyReport report = new WeeklyReport();
        report.setProjectId(projectId);
        report.setStartDate(request.startDate());
        report.setEndDate(request.endDate());
        report.setContentMarkdown(hasText(request.contentMarkdown()) ? request.contentMarkdown() : buildDraftMarkdown(projectId, request));
        return toResponse(weeklyReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public WeeklyReportPreviewResponse preview(UUID projectId, WeeklyReportRequest request) {
        validateDateRange(request);
        projectService.getEntity(projectId);
        return new WeeklyReportPreviewResponse(
                projectId,
                request.startDate(),
                request.endDate(),
                buildDraftMarkdown(projectId, request)
        );
    }

    @Transactional(readOnly = true)
    public List<WeeklyReportResponse> listByProject(UUID projectId) {
        projectService.getEntity(projectId);
        return weeklyReportRepository.findByProjectIdOrderByStartDateDescCreatedAtDesc(projectId).stream()
                .map(WeeklyReportService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyReportResponse get(UUID reportId) {
        return toResponse(weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Weekly report not found: " + reportId)));
    }

    public static String buildMarkdown(
            String projectName,
            LocalDate startDate,
            LocalDate endDate,
            List<Paper> papers,
            List<Experiment> experiments
    ) {
        return buildMarkdown(projectName, startDate, endDate, papers, Collections.emptyMap(), experiments);
    }

    public static String buildMarkdown(
            String projectName,
            LocalDate startDate,
            LocalDate endDate,
            List<Paper> papers,
            Map<UUID, PaperCardInsight> paperInsights,
            List<Experiment> experiments
    ) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(blankToDefault(projectName, "未命名项目")).append(" 周报\n\n");
        markdown.append("周期：").append(startDate).append(" 至 ").append(endDate).append("\n\n");
        markdown.append("## 1. 本周阅读论文\n\n");
        if (papers.isEmpty()) {
            markdown.append("本周暂无更新论文记录。\n\n");
        } else {
            papers.forEach(paper -> appendPaperMarkdown(markdown, paper, paperInsights.get(paper.getId())));
            markdown.append("\n");
        }

        markdown.append("## 2. 本周实验进展\n\n");
        if (experiments.isEmpty()) {
            markdown.append("本周暂无更新实验记录。\n\n");
        } else {
            experiments.forEach(experiment -> markdown
                    .append("- **").append(blankToDefault(experiment.getName(), "未命名实验")).append("**：")
                    .append(experiment.getStatus()).append("\n")
                    .append("  - GitHub 仓库：").append(blankToDefault(experiment.getRepoUrl(), "未填写")).append("\n")
                    .append("  - Commit：`").append(blankToDefault(experiment.getCommitHash(), "未填写")).append("`\n")
                    .append("  - 数据集：").append(blankToDefault(experiment.getDataset(), "未填写")).append("\n")
                    .append("  - 配置：").append(blankToDefault(experiment.getConfig(), "未填写")).append("\n")
                    .append("  - 指标：").append(blankToDefault(experiment.getMetrics(), "未填写")).append("\n")
                    .append("  - 结论：").append(blankToDefault(experiment.getConclusion(), "未填写")).append("\n")
                    .append("  - 失败原因：").append(blankToDefault(experiment.getFailureReason(), "未填写")).append("\n")
                    .append("  - 下一步：").append(blankToDefault(experiment.getNextStep(), "未填写")).append("\n"));
            markdown.append("\n");
        }

        markdown.append("## 3. 当前问题\n\n");
        List<String> blockers = experiments.stream()
                .filter(experiment -> experiment.getStatus() == ExperimentStatus.FAILED)
                .map(Experiment::getFailureReason)
                .filter(WeeklyReportService::hasText)
                .toList();
        if (blockers.isEmpty()) {
            markdown.append("暂无明确阻塞。\n\n");
        } else {
            blockers.forEach(blocker -> markdown.append("- ").append(blocker).append("\n"));
            markdown.append("\n");
        }

        markdown.append("## 4. 下周计划\n\n");
        List<String> nextSteps = experiments.stream()
                .map(Experiment::getNextStep)
                .filter(WeeklyReportService::hasText)
                .distinct()
                .toList();
        if (nextSteps.isEmpty()) {
            markdown.append("继续推进论文阅读和实验验证。\n");
        } else {
            nextSteps.forEach(nextStep -> markdown.append("- ").append(nextStep).append("\n"));
        }
        return markdown.toString();
    }

    private static void appendPaperMarkdown(StringBuilder markdown, Paper paper, PaperCardInsight insight) {
        markdown.append("- **").append(blankToDefault(paper.getTitle(), "未命名论文")).append("**\n")
                .append("  - 作者：").append(blankToDefault(paper.getAuthors(), "未填写")).append("\n")
                .append("  - 标签：").append(blankToDefault(paper.getTags(), "未填写")).append("\n");
        if (insight != null && insight.hasContent()) {
            if (hasText(insight.summary())) {
                markdown.append("  - 摘要：").append(insight.summary()).append("\n");
            }
            appendNestedList(markdown, "主要贡献", insight.contributions());
            appendNestedList(markdown, "局限性", insight.limitations());
        } else {
            markdown.append("  - 关键 claim：").append(blankToDefault(paper.getKeyClaims(), "未填写")).append("\n")
                    .append("  - 笔记：").append(blankToDefault(paper.getNotes(), "未填写")).append("\n");
        }
    }

    private static void appendNestedList(StringBuilder markdown, String title, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        markdown.append("  - ").append(title).append("：\n");
        items.forEach(item -> markdown.append("    - ").append(item).append("\n"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateDateRange(WeeklyReportRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    private String buildDraftMarkdown(UUID projectId, WeeklyReportRequest request) {
        Project project = projectService.getEntity(projectId);
        var start = request.startDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        var end = request.endDate().plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
        List<Paper> papers = paperRepository.findByProjectIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(projectId, start, end);
        List<Experiment> experiments = experimentRepository.findByProjectIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(projectId, start, end);
        return buildMarkdown(project.getName(), request.startDate(), request.endDate(), papers, loadPaperInsights(papers), experiments);
    }

    private Map<UUID, PaperCardInsight> loadPaperInsights(List<Paper> papers) {
        return papers.stream()
                .filter(paper -> paper.getId() != null)
                .map(paper -> new AbstractMap.SimpleEntry<>(paper.getId(), loadPaperInsight(paper.getId())))
                .filter(entry -> entry.getValue() != null && entry.getValue().hasContent())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private PaperCardInsight loadPaperInsight(UUID paperId) {
        return aiJobRepository.findFirstByTargetTypeIgnoreCaseAndTargetIdAndJobTypeIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        "PAPER",
                        paperId,
                        "paper-card",
                        AiJobStatus.SUCCESS
                )
                .map(job -> parsePaperCardInsight(job.getOutputJson()))
                .orElse(null);
    }

    private PaperCardInsight parsePaperCardInsight(String outputJson) {
        if (!hasText(outputJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(outputJson);
            return new PaperCardInsight(
                    text(root.get("summary")),
                    textList(root.get("contributions")),
                    textList(root.get("limitations"))
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("");
    }

    private static List<String> textList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(WeeklyReportService::text)
                .filter(WeeklyReportService::hasText)
                .toList();
    }

    private static String blankToDefault(String value, String fallback) {
        return Objects.requireNonNullElse(value, "").isBlank() ? fallback : value;
    }

    public static WeeklyReportResponse toResponse(WeeklyReport report) {
        return new WeeklyReportResponse(
                report.getId(),
                report.getProjectId(),
                report.getStartDate(),
                report.getEndDate(),
                report.getContentMarkdown(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    public record PaperCardInsight(String summary, List<String> contributions, List<String> limitations) {
        boolean hasContent() {
            return hasText(summary)
                    || (contributions != null && !contributions.isEmpty())
                    || (limitations != null && !limitations.isEmpty());
        }
    }
}
