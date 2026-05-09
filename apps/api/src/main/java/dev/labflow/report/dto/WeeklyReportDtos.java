package dev.labflow.report.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class WeeklyReportDtos {
    private WeeklyReportDtos() {
    }

    public record WeeklyReportRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, String contentMarkdown) {
    }

    public record WeeklyReportPreviewResponse(
            UUID projectId,
            LocalDate startDate,
            LocalDate endDate,
            String contentMarkdown
    ) {
    }

    public record WeeklyReportResponse(
            UUID id,
            UUID projectId,
            LocalDate startDate,
            LocalDate endDate,
            String contentMarkdown,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
