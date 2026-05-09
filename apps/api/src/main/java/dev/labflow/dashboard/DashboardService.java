package dev.labflow.dashboard;

import dev.labflow.dashboard.DashboardDtos.DashboardResponse;
import dev.labflow.dashboard.DashboardDtos.NextAction;
import dev.labflow.dashboard.DashboardDtos.ProjectSummary;
import dev.labflow.dashboard.DashboardDtos.RecentExperiment;
import dev.labflow.dashboard.DashboardDtos.RecentPaper;
import dev.labflow.dashboard.DashboardDtos.RecentReport;
import dev.labflow.experiment.Experiment;
import dev.labflow.experiment.ExperimentRepository;
import dev.labflow.experiment.ExperimentStatus;
import dev.labflow.paper.PaperRepository;
import dev.labflow.project.Project;
import dev.labflow.project.ProjectRepository;
import dev.labflow.report.WeeklyReportRepository;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;
    private final ExperimentRepository experimentRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    public DashboardService(
            ProjectRepository projectRepository,
            PaperRepository paperRepository,
            ExperimentRepository experimentRepository,
            WeeklyReportRepository weeklyReportRepository
    ) {
        this.projectRepository = projectRepository;
        this.paperRepository = paperRepository;
        this.experimentRepository = experimentRepository;
        this.weeklyReportRepository = weeklyReportRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        var projects = projectRepository.findAll().stream()
                .sorted(Comparator.comparing(Project::getUpdatedAt).reversed())
                .toList();
        Map<UUID, Project> projectById = projects.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));

        var summaries = projects.stream()
                .map(project -> new ProjectSummary(
                        project.getId(),
                        project.getName(),
                        project.getDescription(),
                        paperRepository.countByProjectId(project.getId()),
                        experimentRepository.countByProjectId(project.getId()),
                        experimentRepository.countByProjectIdAndStatus(project.getId(), ExperimentStatus.RUNNING),
                        experimentRepository.countByProjectIdAndStatus(project.getId(), ExperimentStatus.FAILED),
                        weeklyReportRepository.countByProjectId(project.getId()),
                        project.getUpdatedAt()
                ))
                .toList();

        var nextActions = experimentRepository.findAll().stream()
                .filter(experiment -> hasText(experiment.getNextStep()))
                .sorted(Comparator.comparing(Experiment::getUpdatedAt).reversed())
                .limit(8)
                .map(experiment -> new NextAction(
                        experiment.getProjectId(),
                        projectName(projectById, experiment.getProjectId()),
                        experiment.getId(),
                        experiment.getName(),
                        experiment.getNextStep(),
                        "normal"
                ))
                .toList();

        return new DashboardResponse(
                projects.size(),
                summaries.stream().filter(summary -> summary.paperCount() > 0 || summary.experimentCount() > 0).count(),
                experimentRepository.countByStatus(ExperimentStatus.RUNNING),
                experimentRepository.countByStatus(ExperimentStatus.FAILED),
                paperRepository.count(),
                weeklyReportRepository.count(),
                summaries,
                nextActions,
                paperRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(paper -> new RecentPaper(
                                paper.getId(),
                                paper.getProjectId(),
                                projectName(projectById, paper.getProjectId()),
                                paper.getTitle(),
                                paper.getTags(),
                                paper.getCreatedAt()
                        ))
                        .toList(),
                experimentRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(experiment -> new RecentExperiment(
                                experiment.getId(),
                                experiment.getProjectId(),
                                projectName(projectById, experiment.getProjectId()),
                                experiment.getName(),
                                experiment.getStatus(),
                                experiment.getNextStep(),
                                experiment.getUpdatedAt()
                        ))
                        .toList(),
                weeklyReportRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(report -> new RecentReport(
                                report.getId(),
                                report.getProjectId(),
                                projectName(projectById, report.getProjectId()),
                                report.getStartDate(),
                                report.getEndDate(),
                                report.getCreatedAt()
                        ))
                        .toList()
        );
    }

    private static String projectName(Map<UUID, Project> projects, UUID projectId) {
        Project project = projects.get(projectId);
        return project == null ? "Unknown project" : project.getName();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
