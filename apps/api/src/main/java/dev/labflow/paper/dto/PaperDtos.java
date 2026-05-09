package dev.labflow.paper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class PaperDtos {
    private PaperDtos() {
    }

    public record PaperRequest(
            @NotBlank String title,
            String authors,
            String url,
            String tags,
            String publication,
            String publishedDate,
            String keyClaims,
            String notes
    ) {
    }

    public record PaperResponse(
            UUID id,
            UUID projectId,
            String title,
            String authors,
            String url,
            String tags,
            String publication,
            String publishedDate,
            String keyClaims,
            String notes,
            UUID pdfFileId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PaperPdfRequest(@NotNull UUID fileId) {
    }

    public record PaperRecognitionTextRequest(@NotBlank String content) {
    }

    public record PaperRecognitionResponse(
            String title,
            String authors,
            String url,
            String tags,
            String publication,
            String publishedDate,
            String keyClaims,
            String notes,
            String source,
            double confidence,
            String warning
    ) {
    }
}
