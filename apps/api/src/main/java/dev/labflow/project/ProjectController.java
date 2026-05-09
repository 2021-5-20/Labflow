package dev.labflow.project;

import dev.labflow.project.dto.ProjectDtos.ProjectRequest;
import dev.labflow.project.dto.ProjectDtos.ProjectResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/projects")
    public List<ProjectResponse> list() {
        return projectService.list();
    }

    @PostMapping("/projects")
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @GetMapping("/projects/{projectId}")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return projectService.get(projectId);
    }

    @PutMapping("/projects/{projectId}")
    public ProjectResponse update(@PathVariable UUID projectId, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(projectId, request);
    }

    @DeleteMapping("/projects/{projectId}")
    public void delete(@PathVariable UUID projectId) {
        projectService.delete(projectId);
    }
}
