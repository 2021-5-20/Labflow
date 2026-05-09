package dev.labflow.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

public final class FileDtos {
    private FileDtos() {
    }

    public record FileRequest(
            @NotBlank String originalFilename,
            String contentType,
            @PositiveOrZero Long sizeBytes,
            @NotBlank String objectKey
    ) {
    }

    public record FileResponse(
            UUID id,
            UUID projectId,
            UUID paperId,
            String originalFilename,
            String contentType,
            Long sizeBytes,
            String objectKey,
            boolean uploadEnabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
