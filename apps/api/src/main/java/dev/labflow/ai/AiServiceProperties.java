package dev.labflow.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "labflow.ai-service")
public record AiServiceProperties(String baseUrl, boolean mockEnabled) {
}
