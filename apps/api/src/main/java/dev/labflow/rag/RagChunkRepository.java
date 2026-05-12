package dev.labflow.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RagChunkRepository extends JpaRepository<RagChunk, UUID> {
    List<RagChunk> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    void deleteByProjectId(UUID projectId);

    long countByProjectId(UUID projectId);
}
