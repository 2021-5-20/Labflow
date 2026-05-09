package dev.labflow.experiment;

import dev.labflow.common.NotFoundException;
import dev.labflow.experiment.dto.ExperimentDtos.ExperimentRequest;
import dev.labflow.experiment.dto.ExperimentDtos.ExperimentResponse;
import dev.labflow.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExperimentService {
    private final ExperimentRepository experimentRepository;
    private final ProjectService projectService;

    public ExperimentService(ExperimentRepository experimentRepository, ProjectService projectService) {
        this.experimentRepository = experimentRepository;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public List<ExperimentResponse> listByProject(UUID projectId) {
        projectService.getEntity(projectId);
        return experimentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(ExperimentService::toResponse)
                .toList();
    }

    @Transactional
    public ExperimentResponse create(UUID projectId, ExperimentRequest request) {
        projectService.getEntity(projectId);
        Experiment experiment = new Experiment();
        experiment.setProjectId(projectId);
        apply(request, experiment);
        return toResponse(experimentRepository.save(experiment));
    }

    @Transactional(readOnly = true)
    public Experiment getEntity(UUID experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new NotFoundException("Experiment not found: " + experimentId));
    }

    @Transactional(readOnly = true)
    public ExperimentResponse get(UUID experimentId) {
        return toResponse(getEntity(experimentId));
    }

    @Transactional
    public ExperimentResponse update(UUID experimentId, ExperimentRequest request) {
        Experiment experiment = getEntity(experimentId);
        apply(request, experiment);
        return toResponse(experimentRepository.save(experiment));
    }

    @Transactional
    public void delete(UUID experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new NotFoundException("Experiment not found: " + experimentId);
        }
        experimentRepository.deleteById(experimentId);
    }

    private static void apply(ExperimentRequest request, Experiment experiment) {
        experiment.setName(request.name());
        experiment.setStatus(request.status());
        experiment.setCommitHash(request.commitHash());
        experiment.setRepoUrl(request.repoUrl());
        experiment.setDataset(request.dataset());
        experiment.setConfig(request.config());
        experiment.setMetrics(request.metrics());
        experiment.setConclusion(request.conclusion());
        experiment.setFailureReason(request.failureReason());
        experiment.setNextStep(request.nextStep());
    }

    public static ExperimentResponse toResponse(Experiment experiment) {
        return new ExperimentResponse(
                experiment.getId(),
                experiment.getProjectId(),
                experiment.getName(),
                experiment.getStatus(),
                experiment.getCommitHash(),
                experiment.getRepoUrl(),
                experiment.getDataset(),
                experiment.getConfig(),
                experiment.getMetrics(),
                experiment.getConclusion(),
                experiment.getFailureReason(),
                experiment.getNextStep(),
                experiment.getCreatedAt(),
                experiment.getUpdatedAt()
        );
    }
}
