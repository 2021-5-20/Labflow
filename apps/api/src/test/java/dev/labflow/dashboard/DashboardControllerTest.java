package dev.labflow.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:labflow_dashboard;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class DashboardControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void dashboardAggregatesProjectsAndNextActions() throws Exception {
        String projectJson = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hybrid Retrieval","description":"Dense and sparse retrieval"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String projectId = projectJson.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/projects/{projectId}/papers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"ColBERT","tags":"retrieval","keyClaims":"Late interaction helps"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/{projectId}/experiments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dense retriever","status":"RUNNING","nextStep":"Finish evaluation"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.runningExperiments").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.projectSummaries", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.nextActions", hasSize(greaterThanOrEqualTo(1))));
    }
}
