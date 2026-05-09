package dev.labflow.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {
    }

    public record ProjectRequest(@NotBlank String name, String description) {
    }

    public record ProjectResponse(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
    }
}
