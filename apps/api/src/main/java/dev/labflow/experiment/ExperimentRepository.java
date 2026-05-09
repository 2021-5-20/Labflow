package dev.labflow.experiment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {
    List<Experiment> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<Experiment> findByProjectIdAndCreatedAtBetweenOrderByCreatedAtAsc(UUID projectId, Instant start, Instant end);

    List<Experiment> findByProjectIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(UUID projectId, Instant start, Instant end);

    List<Experiment> findTop5ByOrderByCreatedAtDesc();

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, ExperimentStatus status);

    long countByStatus(ExperimentStatus status);
}
