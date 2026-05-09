package dev.labflow.paper;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PaperRecognitionServiceTest {
    private final PaperRecognitionService service = new PaperRecognitionService();

    @Test
    void recognizesBibtexExportedFromZotero() {
        String bibtex = """
                @article{lewis2020retrieval,
                  title={Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks},
                  author={Lewis, Patrick and Perez, Ethan and Piktus, Aleksandra},
                  year={2020},
                  doi={10.48550/arXiv.2005.11401},
                  url={https://arxiv.org/abs/2005.11401},
                  keywords={rag, retrieval, generation},
                  abstract={RAG combines parametric and non-parametric memory.},
                  note={Use as baseline for the survey.}
                }
                """;

        var result = service.recognizeZoteroText(bibtex);

        assertThat(result.title()).isEqualTo("Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks");
        assertThat(result.authors()).isEqualTo("Lewis, Patrick and Perez, Ethan and Piktus, Aleksandra");
        assertThat(result.url()).isEqualTo("https://arxiv.org/abs/2005.11401");
        assertThat(result.tags()).isEqualTo("rag, retrieval, generation");
        assertThat(result.publication()).isEqualTo("arXiv");
        assertThat(result.publishedDate()).isEqualTo("2020");
        assertThat(result.keyClaims()).contains("RAG combines");
        assertThat(result.notes()).contains("baseline");
        assertThat(result.source()).isEqualTo("zotero-bibtex");
    }

    @Test
    void recognizesRisExportedFromZotero() {
        String ris = """
                TY  - JOUR
                TI  - A Survey of Retrieval-Augmented Generation
                AU  - Author A
                AU  - Author B
                KW  - rag
                KW  - survey
                JO  - Transactions on Machine Learning Research
                PY  - 2026
                UR  - https://example.com/paper
                AB  - RAG improves factuality by grounding generation.
                N1  - Worth comparing retrievers.
                ER  -
                """;

        var result = service.recognizeZoteroText(ris);

        assertThat(result.title()).isEqualTo("A Survey of Retrieval-Augmented Generation");
        assertThat(result.authors()).isEqualTo("Author A, Author B");
        assertThat(result.url()).isEqualTo("https://example.com/paper");
        assertThat(result.tags()).isEqualTo("rag, survey");
        assertThat(result.publication()).isEqualTo("Transactions on Machine Learning Research");
        assertThat(result.publishedDate()).isEqualTo("2026");
        assertThat(result.keyClaims()).contains("factuality");
        assertThat(result.notes()).contains("retrievers");
        assertThat(result.source()).isEqualTo("zotero-ris");
    }

    @Test
    void fallsBackToReadableTitleFromFilename() {
        var result = service.recognizePdfFilename("2024_retrieval_augmented_generation_survey_v2.pdf");

        assertThat(result.title()).isEqualTo("Retrieval Augmented Generation Survey V2");
        assertThat(result.source()).isEqualTo("pdf-filename");
        assertThat(result.confidence()).isLessThan(0.7);
    }

    @Test
    void recognizesDoiLinkFromPdfText() throws Exception {
        byte[] fakePdfBytes = """
                Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks
                Patrick Lewis, Ethan Perez
                DOI: 10.48550/arXiv.2005.11401
                Published in NeurIPS 2020.
                """.getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "rag-paper.pdf", "application/pdf", fakePdfBytes);

        var result = service.recognizePdfTextFallback(file.getOriginalFilename(), new String(fakePdfBytes, StandardCharsets.UTF_8));

        assertThat(result.title()).isEqualTo("Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks");
        assertThat(result.url()).isEqualTo("https://doi.org/10.48550/arXiv.2005.11401");
        assertThat(result.publication()).isEqualTo("NeurIPS");
        assertThat(result.publishedDate()).isEqualTo("2020");
    }
}
