package dev.labflow.project;

import dev.labflow.common.NotFoundException;
import dev.labflow.file.FileService;
import dev.labflow.project.dto.ProjectDtos.ProjectRequest;
import dev.labflow.project.dto.ProjectDtos.ProjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final FileService fileService;

    public ProjectService(ProjectRepository projectRepository, FileService fileService) {
        this.projectRepository = projectRepository;
        this.fileService = fileService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream().map(ProjectService::toResponse).toList();
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        return toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public Project getEntity(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId) {
        return toResponse(getEntity(projectId));
    }

    @Transactional
    public ProjectResponse update(UUID projectId, ProjectRequest request) {
        Project project = getEntity(projectId);
        project.setName(request.name());
        project.setDescription(request.description());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found: " + projectId);
        }
        fileService.deleteProjectFiles(projectId);
        projectRepository.deleteById(projectId);
    }

    public static ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
