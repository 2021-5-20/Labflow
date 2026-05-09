package dev.labflow.ai;

import dev.labflow.ai.dto.AiJobDtos.AiJobRequest;
import dev.labflow.ai.dto.AiJobDtos.AiJobResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/jobs")
public class AiJobController {
    private final AiJobService aiJobService;

    public AiJobController(AiJobService aiJobService) {
        this.aiJobService = aiJobService;
    }

    @PostMapping
    public AiJobResponse create(@Valid @RequestBody AiJobRequest request) {
        return aiJobService.create(request);
    }

    @GetMapping("/latest")
    public AiJobResponse latest(
            @RequestParam String targetType,
            @RequestParam UUID targetId,
            @RequestParam String jobType
    ) {
        return aiJobService.getLatestSuccessful(targetType, targetId, jobType);
    }

    @GetMapping("/latest-any")
    public AiJobResponse latestAny(
            @RequestParam String targetType,
            @RequestParam UUID targetId,
            @RequestParam String jobType
    ) {
        return aiJobService.getLatest(targetType, targetId, jobType);
    }

    @GetMapping("/{jobId}")
    public AiJobResponse get(@PathVariable UUID jobId) {
        return aiJobService.get(jobId);
    }
}
