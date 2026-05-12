package dev.labflow.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagTextChunkerTest {
    private final RagTextChunker chunker = new RagTextChunker();

    @Test
    void keepsParagraphsAndSectionHeadingsTogetherWhenPossible() {
        String text = """
                Abstract
                Graph neural networks improve NIDS detection by modeling packet relationships.

                Method
                The method builds temporal packet graphs and applies a lightweight graph transformer encoder.

                Experiments
                Evaluation on CICIDS2017 shows better rare attack recall than CNN baselines.
                """;

        var chunks = chunker.split(text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("Abstract\nGraph neural networks");
        assertThat(chunks.get(0)).contains("\n\nMethod\nThe method");
        assertThat(chunks.get(0)).contains("\n\nExperiments\nEvaluation");
    }

    @Test
    void splitsAtParagraphBoundaryInsteadOfCuttingMidParagraph() {
        String firstParagraph = "Abstract\n" + "A".repeat(650);
        String secondParagraph = "Method\n" + "B".repeat(650);
        String thirdParagraph = "Experiments\n" + "C".repeat(650);

        var chunks = chunker.split(String.join("\n\n", firstParagraph, secondParagraph, thirdParagraph));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).contains(firstParagraph);
        assertThat(chunks.get(0)).doesNotContain(thirdParagraph);
        assertThat(chunks.get(1)).contains(thirdParagraph);
        assertThat(chunks.get(1)).doesNotStartWith("C");
    }

    @Test
    void fallsBackToSentenceAwareSplittingForLongParagraphs() {
        String longParagraph = "Method. " + "The graph encoder aggregates packet-level temporal features. ".repeat(55);

        var chunks = chunker.split(longParagraph);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(1600));
        assertThat(chunks.get(0)).endsWith(".");
    }
}
