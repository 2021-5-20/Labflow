package dev.labflow.dashboard;

import dev.labflow.experiment.ExperimentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record DashboardResponse(
            long totalProjects,
            long activeProjects,
            long runningExperiments,
            long failedExperiments,
            long totalPapers,
            long totalReports,
            List<ProjectSummary> projectSummaries,
            List<NextAction> nextActions,
            List<RecentPaper> recentPapers,
            List<RecentExperiment> recentExperiments,
            List<RecentReport> recentReports
    ) {
    }

    public record ProjectSummary(
            UUID id,
            String name,
            String description,
            long paperCount,
            long experimentCount,
            long runningExperiments,
            long failedExperiments,
            long reportCount,
            Instant updatedAt
    ) {
    }

    public record NextAction(
            UUID projectId,
            String projectName,
            UUID experimentId,
            String title,
            String detail,
            String priority
    ) {
    }

    public record RecentPaper(UUID id, UUID projectId, String projectName, String title, String tags, Instant createdAt) {
    }

    public record RecentExperiment(
            UUID id,
            UUID projectId,
            String projectName,
            String name,
            ExperimentStatus status,
            String nextStep,
            Instant updatedAt
    ) {
    }

    public record RecentReport(UUID id, UUID projectId, String projectName, LocalDate startDate, LocalDate endDate, Instant createdAt) {
    }
}
