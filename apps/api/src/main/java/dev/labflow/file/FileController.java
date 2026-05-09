package dev.labflow.file;

import dev.labflow.file.dto.FileDtos.FileRequest;
import dev.labflow.file.dto.FileDtos.FileResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/files/capabilities")
    public Map<String, Object> capabilities() {
        return Map.of(
                "uploadEnabled", true,
                "storage", "minio"
        );
    }

    @PostMapping("/projects/{projectId}/files")
    public FileResponse create(@PathVariable UUID projectId, @Valid @RequestBody FileRequest request) {
        return fileService.create(projectId, request);
    }

    @GetMapping("/projects/{projectId}/files")
    public List<FileResponse> listByProject(@PathVariable UUID projectId) {
        return fileService.listByProject(projectId);
    }

    @GetMapping("/files/{fileId}")
    public FileResponse get(@PathVariable UUID fileId) {
        return fileService.get(fileId);
    }

    @GetMapping("/files/{fileId}/inline")
    public ResponseEntity<InputStreamResource> inline(@PathVariable UUID fileId) {
        FileContent content = fileService.open(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType() == null ? "application/octet-stream" : content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(content.filename())
                        .build()
                        .toString())
                .body(new InputStreamResource(content.stream()));
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
        FileContent content = fileService.open(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType() == null ? "application/octet-stream" : content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(content.filename())
                        .build()
                        .toString())
                .body(new InputStreamResource(content.stream()));
    }

    @DeleteMapping("/files/{fileId}")
    public void delete(@PathVariable UUID fileId) {
        fileService.delete(fileId);
    }
}
