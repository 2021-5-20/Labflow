package dev.labflow.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfTextExtractionService {
    private static final int MAX_PDF_BYTES = 50 * 1024 * 1024;

    public String extract(InputStream stream) throws IOException {
        byte[] bytes = stream.readNBytes(MAX_PDF_BYTES + 1);
        if (bytes.length > MAX_PDF_BYTES) {
            throw new IllegalArgumentException("PDF is too large for AI context extraction");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
