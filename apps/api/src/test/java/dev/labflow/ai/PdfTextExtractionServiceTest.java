package dev.labflow.ai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractionServiceTest {
    private final PdfTextExtractionService service = new PdfTextExtractionService();

    @Test
    void extractsTextFromPdfBytes() throws Exception {
        byte[] pdfBytes = buildPdf("LabFlow PDF context extraction test");

        String text = service.extract(new ByteArrayInputStream(pdfBytes));

        assertThat(text).contains("LabFlow PDF context extraction test");
    }

    private static byte[] buildPdf(String line) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(line);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
