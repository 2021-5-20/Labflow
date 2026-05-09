package dev.labflow.paper;

import dev.labflow.common.NotFoundException;
import dev.labflow.file.FileService;
import dev.labflow.file.StoredFile;
import dev.labflow.paper.dto.PaperDtos.PaperRequest;
import dev.labflow.paper.dto.PaperDtos.PaperPdfRequest;
import dev.labflow.paper.dto.PaperDtos.PaperResponse;
import dev.labflow.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaperService {
    private final PaperRepository paperRepository;
    private final ProjectService projectService;
    private final FileService fileService;

    public PaperService(PaperRepository paperRepository, ProjectService projectService, FileService fileService) {
        this.paperRepository = paperRepository;
        this.projectService = projectService;
        this.fileService = fileService;
    }

    @Transactional(readOnly = true)
    public List<PaperResponse> listByProject(UUID projectId) {
        ensureProjectExists(projectId);
        return paperRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(PaperService::toResponse).toList();
    }

    @Transactional
    public PaperResponse create(UUID projectId, PaperRequest request) {
        ensureProjectExists(projectId);
        Paper paper = new Paper();
        paper.setProjectId(projectId);
        apply(request, paper);
        return toResponse(paperRepository.save(paper));
    }

    @Transactional(readOnly = true)
    public void ensureProjectExists(UUID projectId) {
        projectService.getEntity(projectId);
    }

    @Transactional(readOnly = true)
    public Paper getEntity(UUID paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new NotFoundException("Paper not found: " + paperId));
    }

    @Transactional(readOnly = true)
    public PaperResponse get(UUID paperId) {
        return toResponse(getEntity(paperId));
    }

    @Transactional
    public PaperResponse update(UUID paperId, PaperRequest request) {
        Paper paper = getEntity(paperId);
        apply(request, paper);
        return toResponse(paperRepository.save(paper));
    }

    @Transactional
    public void delete(UUID paperId) {
        Paper paper = getEntity(paperId);
        fileService.deleteLinkedPaperFiles(paper.getId());
        paperRepository.delete(paper);
    }

    @Transactional
    public PaperResponse attachPdf(UUID paperId, PaperPdfRequest request) {
        Paper paper = getEntity(paperId);
        StoredFile file = fileService.getEntity(request.fileId());
        if (!paper.getProjectId().equals(file.getProjectId())) {
            throw new IllegalArgumentException("File does not belong to the same project as paper");
        }
        if (file.getContentType() != null && !file.getContentType().equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("File is not a PDF");
        }
        UUID oldPdfFileId = paper.getPdfFileId();
        fileService.markLinkedToPaper(file, paper.getId());
        paper.setPdfFileId(file.getId());
        PaperResponse response = toResponse(paperRepository.save(paper));
        if (oldPdfFileId != null && !Objects.equals(oldPdfFileId, file.getId())) {
            fileService.deleteLinkedPaperFile(oldPdfFileId, paper.getId());
        }
        return response;
    }

    @Transactional
    public PaperResponse uploadPdf(UUID paperId, MultipartFile file) {
        Paper paper = getEntity(paperId);
        UUID oldPdfFileId = paper.getPdfFileId();
        StoredFile storedFile = fileService.uploadPdf(paper.getProjectId(), paper.getId(), file);
        paper.setPdfFileId(storedFile.getId());
        PaperResponse response = toResponse(paperRepository.save(paper));
        if (oldPdfFileId != null) {
            fileService.deleteLinkedPaperFile(oldPdfFileId, paper.getId());
        }
        return response;
    }

    @Transactional
    public PaperResponse detachPdf(UUID paperId) {
        Paper paper = getEntity(paperId);
        if (paper.getPdfFileId() != null) {
            fileService.clearPaperLink(paper.getPdfFileId(), paper.getId());
            paper.setPdfFileId(null);
        }
        return toResponse(paperRepository.save(paper));
    }

    private static void apply(PaperRequest request, Paper paper) {
        paper.setTitle(request.title());
        paper.setAuthors(request.authors());
        paper.setUrl(request.url());
        paper.setTags(request.tags());
        paper.setPublication(request.publication());
        paper.setPublishedDate(request.publishedDate());
        paper.setKeyClaims(request.keyClaims());
        paper.setNotes(request.notes());
    }

    public static PaperResponse toResponse(Paper paper) {
        return new PaperResponse(
                paper.getId(),
                paper.getProjectId(),
                paper.getTitle(),
                paper.getAuthors(),
                paper.getUrl(),
                paper.getTags(),
                paper.getPublication(),
                paper.getPublishedDate(),
                paper.getKeyClaims(),
                paper.getNotes(),
                paper.getPdfFileId(),
                paper.getCreatedAt(),
                paper.getUpdatedAt()
        );
    }
}
