package dev.labflow.paper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaperRepository extends JpaRepository<Paper, UUID> {
    List<Paper> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<Paper> findByProjectIdAndCreatedAtBetweenOrderByCreatedAtAsc(UUID projectId, Instant start, Instant end);

    List<Paper> findByProjectIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(UUID projectId, Instant start, Instant end);

    List<Paper> findByPdfFileId(UUID pdfFileId);

    List<Paper> findTop5ByOrderByCreatedAtDesc();

    long countByProjectId(UUID projectId);
}
