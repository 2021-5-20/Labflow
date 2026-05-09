package dev.labflow.experiment;

import dev.labflow.experiment.dto.ExperimentDtos.ExperimentRequest;
import dev.labflow.experiment.dto.ExperimentDtos.ExperimentResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ExperimentController {
    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @GetMapping("/projects/{projectId}/experiments")
    public List<ExperimentResponse> listByProject(@PathVariable UUID projectId) {
        return experimentService.listByProject(projectId);
    }

    @PostMapping("/projects/{projectId}/experiments")
    public ExperimentResponse create(@PathVariable UUID projectId, @Valid @RequestBody ExperimentRequest request) {
        return experimentService.create(projectId, request);
    }

    @GetMapping("/experiments/{experimentId}")
    public ExperimentResponse get(@PathVariable UUID experimentId) {
        return experimentService.get(experimentId);
    }

    @PutMapping("/experiments/{experimentId}")
    public ExperimentResponse update(@PathVariable UUID experimentId, @Valid @RequestBody ExperimentRequest request) {
        return experimentService.update(experimentId, request);
    }

    @DeleteMapping("/experiments/{experimentId}")
    public void delete(@PathVariable UUID experimentId) {
        experimentService.delete(experimentId);
    }
}
