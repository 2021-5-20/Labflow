package dev.labflow.ai;

import dev.labflow.ai.dto.AiJobDtos.AiJobRequest;
import dev.labflow.ai.dto.AiJobDtos.AiJobResponse;
import dev.labflow.common.NotFoundException;
import dev.labflow.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class AiJobService {
    private final AiJobRepository aiJobRepository;
    private final ProjectService projectService;
    private final AiJobWorker aiJobWorker;

    public AiJobService(AiJobRepository aiJobRepository, ProjectService projectService, AiJobWorker aiJobWorker) {
        this.aiJobRepository = aiJobRepository;
        this.projectService = projectService;
        this.aiJobWorker = aiJobWorker;
    }

    @Transactional
    public AiJobResponse create(AiJobRequest request) {
        if (request.projectId() != null) {
            projectService.getEntity(request.projectId());
        }
        AiJob job = new AiJob();
        job.setProjectId(request.projectId());
        job.setTargetType(request.targetType());
        job.setTargetId(request.targetId());
        job.setJobType(request.jobType());
        job.setStatus(AiJobStatus.PENDING);
        job.setInputJson(request.inputJson());
        job = aiJobRepository.save(job);
        AiJobResponse response = toResponse(job);
        dispatchAfterCommit(job.getId());
        return response;
    }

    private void dispatchAfterCommit(UUID jobId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            aiJobWorker.execute(jobId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiJobWorker.execute(jobId);
            }
        });
    }

    @Transactional(readOnly = true)
    public AiJobResponse get(UUID jobId) {
        return toResponse(aiJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("AI job not found: " + jobId)));
    }

    @Transactional(readOnly = true)
    public AiJobResponse getLatestSuccessful(String targetType, UUID targetId, String jobType) {
        return toResponse(aiJobRepository
                .findFirstByTargetTypeIgnoreCaseAndTargetIdAndJobTypeIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        targetType,
                        targetId,
                        jobType,
                        AiJobStatus.SUCCESS
                )
                .orElseThrow(() -> new NotFoundException("AI job cache not found")));
    }

    @Transactional(readOnly = true)
    public AiJobResponse getLatest(String targetType, UUID targetId, String jobType) {
        return toResponse(aiJobRepository
                .findFirstByTargetTypeIgnoreCaseAndTargetIdAndJobTypeIgnoreCaseOrderByCreatedAtDesc(
                        targetType,
                        targetId,
                        jobType
                )
                .orElseThrow(() -> new NotFoundException("AI job not found")));
    }

    public static AiJobResponse toResponse(AiJob job) {
        return new AiJobResponse(
                job.getId(),
                job.getProjectId(),
                job.getTargetType(),
                job.getTargetId(),
                job.getJobType(),
                job.getStatus(),
                job.getInputJson(),
                job.getOutputJson(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
