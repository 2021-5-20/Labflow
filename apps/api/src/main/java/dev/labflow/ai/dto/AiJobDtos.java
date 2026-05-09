package dev.labflow.ai.dto;

import dev.labflow.ai.AiJobStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class AiJobDtos {
    private AiJobDtos() {
    }

    public record AiJobRequest(
            UUID projectId,
            @NotBlank String targetType,
            UUID targetId,
            @NotBlank String jobType,
            String inputJson
    ) {
    }

    public record AiJobResponse(
            UUID id,
            UUID projectId,
            String targetType,
            UUID targetId,
            String jobType,
            AiJobStatus status,
            String inputJson,
            String outputJson,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
