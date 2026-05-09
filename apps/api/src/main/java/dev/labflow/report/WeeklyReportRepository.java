package dev.labflow.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, UUID> {
    List<WeeklyReport> findByProjectIdOrderByStartDateDescCreatedAtDesc(UUID projectId);

    List<WeeklyReport> findTop5ByOrderByCreatedAtDesc();

    long countByProjectId(UUID projectId);
}
