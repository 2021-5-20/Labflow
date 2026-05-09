package dev.labflow.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextChunkerTest {
    private final PdfTextChunker chunker = new PdfTextChunker();

    @Test
    void selectsExperimentAndLimitationSnippetsForPaperCard() {
        String text = """
                This introduction gives background and motivation.
                The proposed model uses a graph convolutional architecture.
                Experiments evaluate the method on intrusion detection datasets with several baselines.
                Results show improved detection accuracy under constrained IoT network settings.
                Limitations include limited dataset diversity and unclear generalization to larger networks.
                Future work should evaluate broader traffic traces and stronger baselines.
                """;

        var snippets = chunker.selectSnippets(text, "Graph intrusion detection", "gnn,intrusion");

        assertThat(snippets).isNotEmpty();
        assertThat(String.join(" ", snippets))
                .contains("Experiments")
                .contains("Limitations")
                .contains("Future work");
    }
}
