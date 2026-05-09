package dev.labflow.paper;

import dev.labflow.paper.dto.PaperDtos.PaperRecognitionResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PaperRecognitionService {
    private static final Pattern BIBTEX_FIELD = Pattern.compile("(?ims)^\\s*([a-zA-Z][a-zA-Z0-9_-]*)\\s*=\\s*([\\{\\\"])(.*?)(?<!\\\\)[\\}\\\"]\\s*,?\\s*$");
    private static final Pattern DOI_PATTERN = Pattern.compile("(?i)\\b10\\.\\d{4,9}/[-._;()/:A-Z0-9]+");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern NOISY_FILENAME_TOKEN = Pattern.compile("(?i)\\b(arxiv|paper|final|main|download|accepted|published|copy)\\b");

    public PaperRecognitionResponse recognizeZoteroText(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Recognition content must not be blank");
        }
        if (normalized.startsWith("@")) {
            return recognizeBibtex(normalized);
        }
        if (normalized.lines().anyMatch(line -> line.startsWith("TY  -") || line.startsWith("TI  -"))) {
            return recognizeRis(normalized);
        }
        return new PaperRecognitionResponse(
                firstNonBlankLine(normalized),
                null,
                findFirstUrl(normalized),
                null,
                null,
                null,
                null,
                "从 Zotero 文本导入，未识别到标准 BibTeX/RIS 字段，请人工核对。",
                "zotero-text",
                0.35,
                "未识别到标准 Zotero BibTeX/RIS 格式"
        );
    }

    public PaperRecognitionResponse recognizePdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file must not be empty");
        }
        String filename = file.getOriginalFilename();
        PaperRecognitionResponse fallback = recognizePdfFilename(filename == null ? "paper.pdf" : filename);
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDDocumentInformation info = document.getDocumentInformation();
            String title = clean(info.getTitle());
            String authors = clean(info.getAuthor());
            String tags = clean(info.getKeywords());
            String subject = clean(info.getSubject());
            String extractedText = extractFirstPagesText(document, 2);
            String firstPageTitle = titleFromText(extractedText);
            String resolvedTitle = firstText(title, firstPageTitle, fallback.title());
            String publication = firstText(extractPublication(extractedText), subject);
            String publishedDate = firstText(pdfDate(info), extractPublishedDate(extractedText), extractYear(extractedText));
            String url = firstText(findDoiUrl(extractedText), findArxivUrl(extractedText));
            double confidence = title != null ? 0.78 : firstPageTitle != null ? 0.62 : fallback.confidence();
            return new PaperRecognitionResponse(
                    resolvedTitle,
                    authors,
                    url,
                    tags,
                    publication,
                    publishedDate,
                    subject,
                    "PDF 自动识别结果，请核对标题、作者和摘要字段。",
                    "pdf-metadata",
                    confidence,
                    confidence < 0.7 ? "PDF 元数据不足，部分字段来自首页文本或文件名" : null
            );
        }
    }

    public PaperRecognitionResponse recognizePdfTextFallback(String filename, String text) {
        PaperRecognitionResponse fallback = recognizePdfFilename(filename);
        String title = firstText(titleFromText(text), fallback.title());
        return new PaperRecognitionResponse(
                title,
                extractAuthors(text),
                firstText(findDoiUrl(text), findArxivUrl(text), findFirstUrl(text)),
                fallback.tags(),
                extractPublication(text),
                firstText(extractPublishedDate(text), extractYear(text)),
                null,
                fallback.notes(),
                "pdf-text",
                0.55,
                "PDF 文本启发式识别结果，请人工核对"
        );
    }

    public PaperRecognitionResponse recognizePdfFilename(String filename) {
        String baseName = filename == null ? "" : filename;
        int slash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
        if (slash >= 0) {
            baseName = baseName.substring(slash + 1);
        }
        baseName = baseName.replaceFirst("(?i)\\.pdf$", "");
        String readable = baseName.replaceAll("[_\\-.]+", " ").replaceAll("\\s+", " ").trim();
        readable = readable.replaceFirst("^(19|20)\\d{2}\\s+", "");
        readable = NOISY_FILENAME_TOKEN.matcher(readable).replaceAll("").replaceAll("\\s+", " ").trim();
        if (readable.isBlank()) {
            readable = "Untitled Paper";
        }
        return new PaperRecognitionResponse(
                titleCase(readable),
                null,
                null,
                "pdf",
                null,
                null,
                null,
                "根据 PDF 文件名生成的草稿，请补充作者、claim 和笔记。",
                "pdf-filename",
                0.45,
                "PDF 元数据不可用，已从文件名兜底识别"
        );
    }

    private PaperRecognitionResponse recognizeBibtex(String content) {
        Map<String, String> fields = parseBibtexFields(content);
        String doi = fields.get("doi");
        String url = firstText(fields.get("url"), doi == null ? null : "https://doi.org/" + doi);
        String publication = firstText(
                fields.get("journal"),
                fields.get("journaltitle"),
                fields.get("booktitle"),
                fields.get("publisher"),
                fields.get("archiveprefix"),
                url != null && url.toLowerCase(Locale.ROOT).contains("arxiv.org") ? "arXiv" : null
        );
        return new PaperRecognitionResponse(
                firstText(fields.get("title"), firstNonBlankLine(content)),
                fields.get("author"),
                url,
                firstText(fields.get("keywords"), fields.get("keyword")),
                publication,
                firstText(fields.get("date"), fields.get("year")),
                firstText(fields.get("abstract"), fields.get("summary")),
                firstText(fields.get("note"), fields.get("annote"), fields.get("annotation")),
                "zotero-bibtex",
                0.86,
                null
        );
    }

    private PaperRecognitionResponse recognizeRis(String content) {
        Map<String, List<String>> fields = parseRisFields(content);
        return new PaperRecognitionResponse(
                first(fields, "TI", "T1"),
                String.join(", ", all(fields, "AU", "A1")),
                first(fields, "UR", "DO"),
                String.join(", ", all(fields, "KW")),
                first(fields, "JO", "JF", "T2", "PB"),
                first(fields, "PY", "Y1", "DA"),
                first(fields, "AB", "N2"),
                first(fields, "N1", "NO"),
                "zotero-ris",
                0.82,
                null
        );
    }

    private static Map<String, String> parseBibtexFields(String content) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = BIBTEX_FIELD.matcher(content);
        while (matcher.find()) {
            fields.put(matcher.group(1).toLowerCase(Locale.ROOT), normalizeFieldValue(matcher.group(3)));
        }
        return fields;
    }

    private static Map<String, List<String>> parseRisFields(String content) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        content.lines()
                .map(String::strip)
                .filter(line -> line.length() >= 6 && line.charAt(2) == ' ' && line.charAt(3) == ' ' && line.charAt(4) == '-')
                .forEach(line -> {
                    String key = line.substring(0, 2).toUpperCase(Locale.ROOT);
                    String value = line.substring(6).strip();
                    if (!value.isBlank()) {
                        fields.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
                    }
                });
        return fields;
    }

    private static String extractFirstPagesText(PDDocument document, int pageCount) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(Math.min(pageCount, document.getNumberOfPages()));
        return stripper.getText(document);
    }

    private static String titleFromText(String text) {
        if (text == null) {
            return null;
        }
        return text.lines()
                .map(PaperRecognitionService::clean)
                .filter(line -> line != null && line.length() >= 8 && line.length() <= 220)
                .filter(line -> !line.toLowerCase(Locale.ROOT).contains("arxiv"))
                .findFirst()
                .orElse(null);
    }

    private static String extractAuthors(String text) {
        if (text == null) {
            return null;
        }
        List<String> lines = text.lines().map(PaperRecognitionService::clean).filter(line -> line != null).limit(5).toList();
        return lines.size() >= 2 ? lines.get(1) : null;
    }

    private static String extractPublication(String text) {
        if (text == null) {
            return null;
        }
        Matcher publishedIn = Pattern.compile("(?i)published\\s+in\\s+([^.,\\n]+)").matcher(text);
        if (publishedIn.find()) {
            return clean(publishedIn.group(1).replaceAll("\\b(19|20)\\d{2}\\b", ""));
        }
        Matcher arxiv = Pattern.compile("(?i)\\barxiv\\b").matcher(text);
        return arxiv.find() ? "arXiv" : null;
    }

    private static String extractYear(String text) {
        if (text == null) {
            return null;
        }
        String withoutIdentifiers = DOI_PATTERN.matcher(text).replaceAll(" ");
        withoutIdentifiers = withoutIdentifiers.replaceAll("(?i)arxiv[:\\s]+\\d{4}\\.\\d{4,5}(v\\d+)?", " ");
        Matcher matcher = YEAR_PATTERN.matcher(withoutIdentifiers);
        return matcher.find() ? matcher.group() : null;
    }

    private static String extractPublishedDate(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?i)published\\s+in\\s+[^.\\n]*?\\b((?:19|20)\\d{2})\\b").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String pdfDate(PDDocumentInformation info) {
        if (info == null || info.getCreationDate() == null) {
            return null;
        }
        return String.valueOf(info.getCreationDate().get(Calendar.YEAR));
    }

    private static String first(Map<String, List<String>> fields, String... keys) {
        for (String key : keys) {
            List<String> values = fields.get(key);
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    private static List<String> all(Map<String, List<String>> fields, String... keys) {
        List<String> values = new ArrayList<>();
        for (String key : keys) {
            values.addAll(fields.getOrDefault(key, List.of()));
        }
        return values;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    private static String firstNonBlankLine(String value) {
        return value.lines().map(PaperRecognitionService::clean).filter(line -> line != null).findFirst().orElse("Untitled Paper");
    }

    private static String findFirstUrl(String value) {
        Matcher matcher = Pattern.compile("https?://\\S+").matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    private static String findDoiUrl(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = DOI_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        return "https://doi.org/" + matcher.group().replaceAll("[).,;]+$", "");
    }

    private static String findArxivUrl(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?i)arxiv[:\\s]+(\\d{4}\\.\\d{4,5})(v\\d+)?").matcher(value);
        return matcher.find() ? "https://arxiv.org/abs/" + matcher.group(1) : null;
    }

    private static String normalizeFieldValue(String value) {
        return clean(value == null ? null : value.replaceAll("\\s+", " ").replace("{", "").replace("}", ""));
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").strip();
        return cleaned.isBlank() ? null : cleaned;
    }

    private static String titleCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (String token : value.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            if (token.length() <= 2 && token.chars().allMatch(Character::isUpperCase)) {
                builder.append(token);
            } else {
                builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
            }
        }
        return builder.toString();
    }
}
