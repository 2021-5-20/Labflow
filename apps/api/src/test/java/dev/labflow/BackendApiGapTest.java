package dev.labflow;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:labflow_gap;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "labflow.ai-service.mock-enabled=true"
})
class BackendApiGapTest {
    private static final Pattern ID_PATTERN = Pattern.compile(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", Pattern.DOTALL);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    MinioClient minioClient;

    @Test
    void invalidWeeklyReportDateRangeReturnsBadRequest() throws Exception {
        String projectId = createProject();

        mockMvc.perform(post("/api/projects/{projectId}/reports/weekly", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-05-08","endDate":"2026-05-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error", containsString("endDate")));
    }

    @Test
    void weeklyReportIncludesPapersAndExperimentsUpdatedInsideRange() throws Exception {
        String projectId = createProject();
        String paperId = extractId(mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated Paper","notes":"Important new note"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String experimentId = extractId(mockMvc.perform(post("/api/projects/{projectId}/experiments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Experiment","status":"SUCCESS","nextStep":"Write ablation summary"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        jdbcTemplate.update(
                "update papers set created_at = TIMESTAMP WITH TIME ZONE '2026-04-01 00:00:00Z', updated_at = TIMESTAMP WITH TIME ZONE '2026-05-03 00:00:00Z' where id = ?",
                paperId
        );
        jdbcTemplate.update(
                "update experiments set created_at = TIMESTAMP WITH TIME ZONE '2026-04-01 00:00:00Z', updated_at = TIMESTAMP WITH TIME ZONE '2026-05-04 00:00:00Z' where id = ?",
                experimentId
        );

        mockMvc.perform(post("/api/projects/{projectId}/reports/weekly", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-05-01","endDate":"2026-05-08"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentMarkdown", containsString("Updated Paper")))
                .andExpect(jsonPath("$.contentMarkdown", containsString("Updated Experiment")));
    }

    @Test
    void weeklyReportPreviewDoesNotPersistHistory() throws Exception {
        String projectId = createProject();

        mockMvc.perform(post("/api/projects/{projectId}/reports/weekly/preview", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-05-01","endDate":"2026-05-08"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentMarkdown", containsString("API Gap Project 周报")));

        mockMvc.perform(get("/api/projects/{projectId}/reports", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void confirmedWeeklyReportPersistsEditedMarkdown() throws Exception {
        String projectId = createProject();

        mockMvc.perform(post("/api/projects/{projectId}/reports/weekly", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate":"2026-05-01",
                                  "endDate":"2026-05-08",
                                  "contentMarkdown":"# Edited weekly report\\n\\nManual notes."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentMarkdown").value("# Edited weekly report\n\nManual notes."));
    }

    @Test
    void weeklyReportPreviewUsesCachedAiPaperCardWhenAvailable() throws Exception {
        String projectId = createProject();
        String paperId = extractId(mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Paper With AI Card","keyClaims":"Original claim","notes":"Original notes"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        String jobId = extractId(mockMvc.perform(post("/api/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "targetType":"PAPER",
                                  "targetId":"%s",
                                  "jobType":"paper-card",
                                  "inputJson":"{\\"paperId\\":\\"%s\\",\\"title\\":\\"Paper With AI Card\\"}"
                                }
                                """.formatted(projectId, paperId, paperId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        awaitAiJobSuccess(jobId);

        mockMvc.perform(post("/api/projects/{projectId}/reports/weekly/preview", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-05-01","endDate":"2026-05-10"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentMarkdown", containsString("摘要：模拟论文摘要。")))
                .andExpect(jsonPath("$.contentMarkdown", not(containsString("AI 摘要"))))
                .andExpect(jsonPath("$.contentMarkdown", containsString("主要贡献")))
                .andExpect(jsonPath("$.contentMarkdown", containsString("模拟主要贡献 1")))
                .andExpect(jsonPath("$.contentMarkdown", not(containsString("AI 主要贡献"))))
                .andExpect(jsonPath("$.contentMarkdown", containsString("局限性")))
                .andExpect(jsonPath("$.contentMarkdown", not(containsString("AI 局限性"))))
                .andExpect(jsonPath("$.contentMarkdown", containsString("模拟局限性")));
    }

    @Test
    void fileMetadataCanBeAssociatedWithPaperPdf() throws Exception {
        String projectId = createProject();
        String fileJson = mockMvc.perform(post("/api/projects/{projectId}/files", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalFilename":"paper.pdf","contentType":"application/pdf","sizeBytes":4096,"objectKey":"papers/paper.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.uploadEnabled").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String fileId = extractId(fileJson);

        String paperId = extractId(mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Paper With PDF"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        mockMvc.perform(put("/api/papers/{paperId}/pdf", paperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":\"" + fileId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfFileId").value(fileId));

        mockMvc.perform(get("/api/projects/{projectId}/files", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fileId));

        mockMvc.perform(delete("/api/papers/{paperId}/pdf", paperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfFileId").doesNotExist());
    }

    @Test
    void deletingPaperRemovesLinkedPdfMetadataAndMinioObject() throws Exception {
        String projectId = createProject();
        String objectKey = "papers/delete-me.pdf";
        String fileJson = mockMvc.perform(post("/api/projects/{projectId}/files", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalFilename":"delete-me.pdf","contentType":"application/pdf","sizeBytes":4096,"objectKey":"%s"}
                                """.formatted(objectKey)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String fileId = extractId(fileJson);

        String paperId = extractId(mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Paper To Delete"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        mockMvc.perform(put("/api/papers/{paperId}/pdf", paperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":\"" + fileId + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/papers/{paperId}", paperId))
                .andExpect(status().isOk());

        Integer fileCount = jdbcTemplate.queryForObject("select count(*) from stored_files where id = ?", Integer.class, fileId);
        assertThat(fileCount).isZero();
        verify(minioClient).removeObject(argThat((RemoveObjectArgs args) -> objectKey.equals(args.object())));
    }

    @Test
    void deletingProjectRemovesProjectFilesFromMinio() throws Exception {
        String projectId = createProject();
        String objectKey = "projects/delete-project/paper.pdf";
        String fileJson = mockMvc.perform(post("/api/projects/{projectId}/files", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalFilename":"paper.pdf","contentType":"application/pdf","sizeBytes":4096,"objectKey":"%s"}
                                """.formatted(objectKey)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String fileId = extractId(fileJson);

        mockMvc.perform(delete("/api/projects/{projectId}", projectId))
                .andExpect(status().isOk());

        Integer fileCount = jdbcTemplate.queryForObject("select count(*) from stored_files where id = ?", Integer.class, fileId);
        assertThat(fileCount).isZero();
        verify(minioClient).removeObject(argThat((RemoveObjectArgs args) -> objectKey.equals(args.object())));
    }

    @Test
    void replacingPaperPdfRemovesOldMinioObjectAndMetadata() throws Exception {
        String projectId = createProject();
        String oldObjectKey = "papers/old-paper.pdf";
        String oldFileJson = mockMvc.perform(post("/api/projects/{projectId}/files", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalFilename":"old-paper.pdf","contentType":"application/pdf","sizeBytes":4096,"objectKey":"%s"}
                                """.formatted(oldObjectKey)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String oldFileId = extractId(oldFileJson);

        String paperId = extractId(mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Paper With Replaceable PDF"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        mockMvc.perform(put("/api/papers/{paperId}/pdf", paperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":\"" + oldFileId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfFileId").value(oldFileId));

        MockMultipartFile replacement = new MockMultipartFile(
                "file",
                "replacement.pdf",
                "application/pdf",
                "%PDF-1.4 replacement".getBytes()
        );

        mockMvc.perform(multipart("/api/papers/{paperId}/pdf/upload", paperId).file(replacement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfFileId").exists());

        Integer oldFileCount = jdbcTemplate.queryForObject("select count(*) from stored_files where id = ?", Integer.class, oldFileId);
        assertThat(oldFileCount).isZero();
        verify(minioClient).removeObject(argThat((RemoveObjectArgs args) -> oldObjectKey.equals(args.object())));
    }

    @Test
    void experimentAcceptsGithubRepoUrl() throws Exception {
        String projectId = createProject();

        mockMvc.perform(post("/api/projects/{projectId}/experiments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"gnn_baseline",
                                  "status":"RUNNING",
                                  "repoUrl":"https://github.com/example/labflow-experiments",
                                  "dataset":"cicids2017"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repoUrl").value("https://github.com/example/labflow-experiments"));
    }

    @Test
    void aiJobExecutesMockPaperCardAndStoresOutput() throws Exception {
        String projectId = createProject();

        String jobId = extractId(mockMvc.perform(post("/api/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "targetType":"paper",
                                  "jobType":"paper-card",
                                  "inputJson":"{\\"paperId\\":\\"paper-1\\",\\"title\\":\\"A Paper\\",\\"notes\\":\\"notes\\"}"
                                }
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        awaitAiJobSuccess(jobId);
        mockMvc.perform(get("/api/ai/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.outputJson", containsString("模拟论文摘要")));
    }

    @Test
    void aiJobCreateReturnsPendingBeforeAsyncExecutionCompletes() throws Exception {
        String projectId = createProject();

        String jobId = extractId(mockMvc.perform(post("/api/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "targetType":"paper",
                                  "jobType":"paper-card",
                                  "inputJson":"{\\"paperId\\":\\"paper-1\\",\\"title\\":\\"A Paper\\"}"
                                }
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.outputJson").value(nullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void latestAiJobReturnsCachedPaperCardForTarget() throws Exception {
        String projectId = createProject();
        String paperId = extractId(mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cached Paper Card"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        String jobId = extractId(mockMvc.perform(post("/api/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "targetType":"PAPER",
                                  "targetId":"%s",
                                  "jobType":"paper-card",
                                  "inputJson":"{\\"paperId\\":\\"%s\\",\\"title\\":\\"Cached Paper Card\\"}"
                                }
                                """.formatted(projectId, paperId, paperId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        awaitAiJobSuccess(jobId);

        mockMvc.perform(get("/api/ai/jobs/latest")
                        .param("targetType", "PAPER")
                        .param("targetId", paperId)
                        .param("jobType", "paper-card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(paperId))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.outputJson", containsString("模拟论文摘要")));
    }

    @Test
    void latestAnyAiJobReturnsPendingJobForTarget() throws Exception {
        String projectId = createProject();
        String paperId = extractId(mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Pending Paper Card"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String jobId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                        insert into ai_jobs
                        (id, project_id, target_type, target_id, job_type, status, input_json, created_at, updated_at)
                        values (?, ?, 'PAPER', ?, 'paper-card', 'PENDING', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                jobId,
                projectId,
                paperId
        );

        mockMvc.perform(get("/api/ai/jobs/latest-any")
                        .param("targetType", "PAPER")
                        .param("targetId", paperId)
                        .param("jobType", "paper-card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void aiJobExecutesMockWeeklyReportPolishAndStoresMarkdown() throws Exception {
        String projectId = createProject();

        String jobId = extractId(mockMvc.perform(post("/api/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "targetType":"WEEKLY_REPORT",
                                  "jobType":"weekly-report-polish",
                                  "inputJson":"{\\"contentMarkdown\\":\\"# Draft Report\\\\n\\\\n- Raw note\\"}"
                                }
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString());

        awaitAiJobSuccess(jobId);
        mockMvc.perform(get("/api/ai/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.outputJson", containsString("# Draft Report")))
                .andExpect(jsonPath("$.outputJson", containsString("Mock AI polished weekly report")));
    }

    private void awaitAiJobSuccess(String jobId) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/ai/jobs/{jobId}", jobId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("SUCCESS"))
        );
    }

    private String createProject() throws Exception {
        return extractId(mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"API Gap Project","description":"backend gap test"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    private static String extractId(String json) {
        var matcher = ID_PATTERN.matcher(json);
        assertThat(matcher.matches()).isTrue();
        return matcher.group(1);
    }
}
