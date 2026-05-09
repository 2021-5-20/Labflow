package dev.labflow.experiment.dto;

import dev.labflow.experiment.ExperimentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class ExperimentDtos {
    private ExperimentDtos() {
    }

    public record ExperimentRequest(
            @NotBlank String name,
            @NotNull ExperimentStatus status,
            String commitHash,
            String repoUrl,
            String dataset,
            String config,
            String metrics,
            String conclusion,
            String failureReason,
            String nextStep
    ) {
    }

    public record ExperimentResponse(
            UUID id,
            UUID projectId,
            String name,
            ExperimentStatus status,
            String commitHash,
            String repoUrl,
            String dataset,
            String config,
            String metrics,
            String conclusion,
            String failureReason,
            String nextStep,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
