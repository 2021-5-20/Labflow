package dev.labflow.rag.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RagDtos {
    private RagDtos() {
    }

    public record RagIndexResponse(
            UUID projectId,
            int chunkCount,
            Instant indexedAt
    ) {
    }

    public record RagQuestionRequest(
            @NotBlank String question
    ) {
    }

    public record RagSearchResponse(
            UUID projectId,
            String question,
            List<RagChunkResponse> results
    ) {
    }

    public record RagAskResponse(
            UUID projectId,
            String question,
            String answerMarkdown,
            List<RagChunkResponse> contexts
    ) {
    }

    public record RagChunkResponse(
            UUID id,
            UUID projectId,
            String sourceType,
            UUID sourceId,
            String sourceTitle,
            String contentText,
            double score,
            String metadataJson,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
