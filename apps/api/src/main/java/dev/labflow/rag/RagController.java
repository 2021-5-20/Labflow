package dev.labflow.rag;

import dev.labflow.rag.dto.RagDtos.RagAskResponse;
import dev.labflow.rag.dto.RagDtos.RagChunkResponse;
import dev.labflow.rag.dto.RagDtos.RagIndexResponse;
import dev.labflow.rag.dto.RagDtos.RagQuestionRequest;
import dev.labflow.rag.dto.RagDtos.RagSearchResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/rag")
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/index")
    public RagIndexResponse index(@PathVariable UUID projectId) {
        return ragService.indexProject(projectId);
    }

    @GetMapping("/chunks")
    public List<RagChunkResponse> chunks(@PathVariable UUID projectId) {
        return ragService.listChunks(projectId);
    }

    @PostMapping("/search")
    public RagSearchResponse search(@PathVariable UUID projectId, @Valid @RequestBody RagQuestionRequest request) {
        return ragService.search(projectId, request.question());
    }

    @PostMapping("/ask")
    public RagAskResponse ask(@PathVariable UUID projectId, @Valid @RequestBody RagQuestionRequest request) {
        return ragService.ask(projectId, request.question());
    }
}
