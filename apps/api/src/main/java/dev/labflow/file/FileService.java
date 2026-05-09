package dev.labflow.file;

import dev.labflow.common.NotFoundException;
import dev.labflow.file.dto.FileDtos.FileRequest;
import dev.labflow.file.dto.FileDtos.FileResponse;
import dev.labflow.project.ProjectRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    private final StoredFileRepository storedFileRepository;
    private final ProjectRepository projectRepository;
    private final MinioClient minioClient;
    private final String bucket;

    public FileService(
            StoredFileRepository storedFileRepository,
            ProjectRepository projectRepository,
            MinioClient minioClient,
            @Value("${labflow.minio.bucket}") String bucket
    ) {
        this.storedFileRepository = storedFileRepository;
        this.projectRepository = projectRepository;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Transactional
    public FileResponse create(UUID projectId, FileRequest request) {
        ensureProjectExists(projectId);
        StoredFile file = new StoredFile();
        file.setProjectId(projectId);
        file.setOriginalFilename(request.originalFilename());
        file.setContentType(request.contentType());
        file.setSizeBytes(request.sizeBytes());
        file.setObjectKey(request.objectKey());
        return toResponse(storedFileRepository.save(file));
    }

    @Transactional
    public StoredFile uploadPdf(UUID projectId, UUID paperId, MultipartFile multipartFile) {
        ensureProjectExists(projectId);
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("PDF file must not be empty");
        }
        String filename = multipartFile.getOriginalFilename() == null ? "paper.pdf" : multipartFile.getOriginalFilename();
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files can be uploaded");
        }
        String contentType = multipartFile.getContentType() == null ? "application/pdf" : multipartFile.getContentType();
        String objectKey = buildPaperPdfObjectKey(projectId, paperId, filename);
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload PDF to MinIO", ex);
        }

        StoredFile file = new StoredFile();
        file.setProjectId(projectId);
        file.setPaperId(paperId);
        file.setOriginalFilename(filename);
        file.setContentType(contentType);
        file.setSizeBytes(multipartFile.getSize());
        file.setObjectKey(objectKey);
        return storedFileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listByProject(UUID projectId) {
        ensureProjectExists(projectId);
        return storedFileRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(FileService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FileResponse get(UUID fileId) {
        return toResponse(getEntity(fileId));
    }

    @Transactional(readOnly = true)
    public StoredFile getEntity(UUID fileId) {
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found: " + fileId));
    }

    @Transactional
    public void delete(UUID fileId) {
        StoredFile file = getEntity(fileId);
        if (file.getPaperId() != null) {
            throw new IllegalArgumentException("File is linked to a paper and cannot be deleted");
        }
        deleteObjectAndMetadata(file);
    }

    @Transactional
    public void deleteLinkedPaperFiles(UUID paperId) {
        List<StoredFile> files = storedFileRepository.findByPaperId(paperId);
        files.forEach(this::deleteObjectAndMetadata);
    }

    @Transactional
    public void deleteLinkedPaperFile(UUID fileId, UUID paperId) {
        StoredFile file = getEntity(fileId);
        if (paperId.equals(file.getPaperId())) {
            deleteObjectAndMetadata(file);
        }
    }

    @Transactional
    public void deleteProjectFiles(UUID projectId) {
        ensureProjectExists(projectId);
        storedFileRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .forEach(this::deleteObjectAndMetadata);
    }

    @Transactional
    public void markLinkedToPaper(StoredFile file, UUID paperId) {
        file.setPaperId(paperId);
        storedFileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public FileContent open(UUID fileId) {
        StoredFile file = getEntity(fileId);
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(file.getObjectKey())
                    .build());
            return new FileContent(stream, file.getOriginalFilename(), file.getContentType(), file.getSizeBytes());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read file from MinIO", ex);
        }
    }

    @Transactional
    public void clearPaperLink(UUID fileId, UUID paperId) {
        StoredFile file = getEntity(fileId);
        if (paperId.equals(file.getPaperId())) {
            file.setPaperId(null);
            storedFileRepository.save(file);
        }
    }

    public static FileResponse toResponse(StoredFile file) {
        return new FileResponse(
                file.getId(),
                file.getProjectId(),
                file.getPaperId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getObjectKey(),
                true,
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }

    public static String buildPaperPdfObjectKey(UUID projectId, UUID paperId, String filename) {
        String safeFilename = filename == null ? "paper.pdf" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "projects/%s/papers/%s/%s-%s".formatted(projectId, paperId, UUID.randomUUID(), safeFilename);
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private void ensureProjectExists(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found: " + projectId);
        }
    }

    private void deleteObjectAndMetadata(StoredFile file) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(file.getObjectKey())
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete file from MinIO", ex);
        }
        storedFileRepository.delete(file);
    }
}
