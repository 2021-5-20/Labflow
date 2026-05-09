package dev.labflow.paper;

import dev.labflow.paper.dto.PaperDtos.PaperRequest;
import dev.labflow.paper.dto.PaperDtos.PaperPdfRequest;
import dev.labflow.paper.dto.PaperDtos.PaperRecognitionResponse;
import dev.labflow.paper.dto.PaperDtos.PaperRecognitionTextRequest;
import dev.labflow.paper.dto.PaperDtos.PaperResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PaperController {
    private final PaperService paperService;
    private final PaperRecognitionService paperRecognitionService;

    public PaperController(PaperService paperService, PaperRecognitionService paperRecognitionService) {
        this.paperService = paperService;
        this.paperRecognitionService = paperRecognitionService;
    }

    @GetMapping("/projects/{projectId}/papers")
    public List<PaperResponse> listByProject(@PathVariable UUID projectId) {
        return paperService.listByProject(projectId);
    }

    @PostMapping("/projects/{projectId}/papers")
    public PaperResponse create(@PathVariable UUID projectId, @Valid @RequestBody PaperRequest request) {
        return paperService.create(projectId, request);
    }

    @GetMapping("/papers/{paperId}")
    public PaperResponse get(@PathVariable UUID paperId) {
        return paperService.get(paperId);
    }

    @PutMapping("/papers/{paperId}")
    public PaperResponse update(@PathVariable UUID paperId, @Valid @RequestBody PaperRequest request) {
        return paperService.update(paperId, request);
    }

    @DeleteMapping("/papers/{paperId}")
    public void delete(@PathVariable UUID paperId) {
        paperService.delete(paperId);
    }

    @PostMapping(value = "/papers/{paperId}/pdf/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PaperResponse uploadPdf(@PathVariable UUID paperId, @RequestPart("file") MultipartFile file) {
        return paperService.uploadPdf(paperId, file);
    }

    @PostMapping(value = "/projects/{projectId}/papers/recognize/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PaperRecognitionResponse recognizePdf(@PathVariable UUID projectId, @RequestPart("file") MultipartFile file) throws IOException {
        paperService.ensureProjectExists(projectId);
        return paperRecognitionService.recognizePdf(file);
    }

    @PostMapping("/projects/{projectId}/papers/recognize/zotero")
    public PaperRecognitionResponse recognizeZotero(@PathVariable UUID projectId, @Valid @RequestBody PaperRecognitionTextRequest request) {
        paperService.ensureProjectExists(projectId);
        return paperRecognitionService.recognizeZoteroText(request.content());
    }

    @PutMapping("/papers/{paperId}/pdf")
    public PaperResponse attachPdf(@PathVariable UUID paperId, @Valid @RequestBody PaperPdfRequest request) {
        return paperService.attachPdf(paperId, request);
    }

    @DeleteMapping("/papers/{paperId}/pdf")
    public PaperResponse detachPdf(@PathVariable UUID paperId) {
        return paperService.detachPdf(paperId);
    }
}
