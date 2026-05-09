package dev.labflow.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {
    Optional<AiJob> findFirstByTargetTypeIgnoreCaseAndTargetIdAndJobTypeIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String targetType,
            UUID targetId,
            String jobType,
            AiJobStatus status
    );

    Optional<AiJob> findFirstByTargetTypeIgnoreCaseAndTargetIdAndJobTypeIgnoreCaseOrderByCreatedAtDesc(
            String targetType,
            UUID targetId,
            String jobType
    );
}
