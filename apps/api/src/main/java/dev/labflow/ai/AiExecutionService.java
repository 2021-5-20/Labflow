package dev.labflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Service
public class AiExecutionService {
    private final AiServiceProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PaperAiContextService paperAiContextService;

    public AiExecutionService(
            AiServiceProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            PaperAiContextService paperAiContextService
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.paperAiContextService = paperAiContextService;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    public String execute(AiJob job) {
        String inputJson = job.getInputJson() == null || job.getInputJson().isBlank() ? "{}" : job.getInputJson();
        String jobType = job.getJobType().trim().toLowerCase();
        if (properties.mockEnabled()) {
            return mockResponse(jobType, inputJson);
        }
        String path = switch (jobType) {
            case "paper-card" -> "/paper-card/generate";
            case "weekly-report-polish" -> "/weekly-report/polish";
            default -> throw new IllegalArgumentException("Unsupported AI job type: " + job.getJobType());
        };
        try {
            JsonNode input = objectMapper.readTree(inputJson);
            String requestBody = "paper-card".equals(jobType)
                    ? paperAiContextService.enrichPaperCardInput(job, input)
                    : inputJson;
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("AI job inputJson must be valid JSON", ex);
        }
    }

    private String mockResponse(String jobType, String inputJson) {
        ObjectNode response = objectMapper.createObjectNode();
        switch (jobType) {
            case "paper-card" -> {
                response.put("summary", "模拟论文摘要。");
                ArrayNode contributions = response.putArray("contributions");
                contributions.add("模拟主要贡献 1");
                contributions.add("模拟主要贡献 2");
                response.putArray("limitations").add("模拟局限性");
                response.putArray("citations");
            }
            case "weekly-report-polish" -> response.put("contentMarkdown", extractContentMarkdown(inputJson)
                    + "\n\n> Mock AI polished weekly report.");
            default -> throw new IllegalArgumentException("Unsupported AI job type: " + jobType);
        }
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize mock AI response", ex);
        }
    }

    private String extractContentMarkdown(String inputJson) {
        try {
            JsonNode input = objectMapper.readTree(inputJson);
            JsonNode contentMarkdown = input.get("contentMarkdown");
            if (contentMarkdown != null && contentMarkdown.isTextual()) {
                return contentMarkdown.asText();
            }
        } catch (JsonProcessingException ignored) {
            // Validated by execute() for real calls; mock keeps a readable fallback for direct tests.
        }
        return inputJson;
    }
}
