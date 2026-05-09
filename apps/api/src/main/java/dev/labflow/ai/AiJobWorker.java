package dev.labflow.ai;

import dev.labflow.common.NotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiJobWorker {
    private final AiJobRepository aiJobRepository;
    private final AiExecutionService aiExecutionService;

    public AiJobWorker(AiJobRepository aiJobRepository, AiExecutionService aiExecutionService) {
        this.aiJobRepository = aiJobRepository;
        this.aiExecutionService = aiExecutionService;
    }

    @Async
    @Transactional
    public void execute(UUID jobId) {
        AiJob job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("AI job not found: " + jobId));
        job.setStatus(AiJobStatus.RUNNING);
        job.setErrorMessage(null);
        job.setOutputJson(null);
        aiJobRepository.saveAndFlush(job);

        try {
            job.setOutputJson(aiExecutionService.execute(job));
            job.setStatus(AiJobStatus.SUCCESS);
            job.setErrorMessage(null);
        } catch (RuntimeException ex) {
            job.setStatus(AiJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
        }
        aiJobRepository.save(job);
    }
}
