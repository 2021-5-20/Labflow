package dev.labflow;

import dev.labflow.ai.AiServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AiServiceProperties.class)
public class LabFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabFlowApplication.class, args);
    }
}
