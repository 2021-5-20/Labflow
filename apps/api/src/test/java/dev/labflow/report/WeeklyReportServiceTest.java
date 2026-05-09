package dev.labflow.report;

import dev.labflow.experiment.Experiment;
import dev.labflow.experiment.ExperimentStatus;
import dev.labflow.paper.Paper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReportServiceTest {
    @Test
    void buildMarkdownSummarizesPapersExperimentsBlockersAndNextSteps() {
        Paper paper = new Paper();
        paper.setTitle("A Survey of Retrieval-Augmented Generation");
        paper.setAuthors("Author A, Author B");
        paper.setTags("rag,survey,llm");
        paper.setKeyClaims("RAG improves factuality.");
        paper.setNotes("Compare dense and hybrid retrieval.");

        Experiment failed = new Experiment();
        failed.setName("reranker_ablation_v1");
        failed.setStatus(ExperimentStatus.FAILED);
        failed.setRepoUrl("https://github.com/example/rag-survey");
        failed.setCommitHash("abc1234");
        failed.setDataset("nq_dev");
        failed.setConfig("top_k=20");
        failed.setMetrics("em=0.421");
        failed.setConclusion("Needs more data.");
        failed.setFailureReason("GPU memory exceeded.");
        failed.setNextStep("Reduce batch size.");

        String markdown = WeeklyReportService.buildMarkdown(
                "RAG Survey",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 8),
                List.of(paper),
                List.of(failed)
        );

        assertThat(markdown).contains("# RAG Survey 周报");
        assertThat(markdown).contains("**A Survey of Retrieval-Augmented Generation**");
        assertThat(markdown).contains("**reranker_ablation_v1**：FAILED");
        assertThat(markdown).contains("GitHub 仓库：https://github.com/example/rag-survey");
        assertThat(markdown).contains("- GPU memory exceeded.");
        assertThat(markdown).contains("- Reduce batch size.");
    }

    @Test
    void buildMarkdownUsesFallbacksWhenThereAreNoBlockersOrNextSteps() {
        String markdown = WeeklyReportService.buildMarkdown(
                "RAG Survey",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 8),
                List.of(),
                List.of()
        );

        assertThat(markdown).contains("暂无明确阻塞。");
        assertThat(markdown).contains("继续推进论文阅读和实验验证。");
    }
}
