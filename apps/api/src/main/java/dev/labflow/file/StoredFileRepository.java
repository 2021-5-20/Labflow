package dev.labflow.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    List<StoredFile> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<StoredFile> findByPaperId(UUID paperId);
}
