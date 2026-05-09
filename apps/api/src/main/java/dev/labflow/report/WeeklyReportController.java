package dev.labflow.report;

import dev.labflow.report.dto.WeeklyReportDtos.WeeklyReportRequest;
import dev.labflow.report.dto.WeeklyReportDtos.WeeklyReportPreviewResponse;
import dev.labflow.report.dto.WeeklyReportDtos.WeeklyReportResponse;
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
@RequestMapping("/api")
public class WeeklyReportController {
    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @PostMapping("/projects/{projectId}/reports/weekly")
    public WeeklyReportResponse generate(@PathVariable UUID projectId, @Valid @RequestBody WeeklyReportRequest request) {
        return weeklyReportService.generate(projectId, request);
    }

    @PostMapping("/projects/{projectId}/reports/weekly/preview")
    public WeeklyReportPreviewResponse preview(@PathVariable UUID projectId, @Valid @RequestBody WeeklyReportRequest request) {
        return weeklyReportService.preview(projectId, request);
    }

    @GetMapping("/projects/{projectId}/reports")
    public List<WeeklyReportResponse> listByProject(@PathVariable UUID projectId) {
        return weeklyReportService.listByProject(projectId);
    }

    @GetMapping("/reports/{reportId}")
    public WeeklyReportResponse get(@PathVariable UUID reportId) {
        return weeklyReportService.get(reportId);
    }
}
