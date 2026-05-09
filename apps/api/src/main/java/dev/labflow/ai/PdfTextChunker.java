package dev.labflow.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PdfTextChunker {
    private static final int CHUNK_SIZE = 1400;
    private static final int CHUNK_OVERLAP = 180;
    private static final int MAX_SNIPPETS = 10;
    private static final int MAX_SNIPPET_CHARS = 1600;
    private static final int MAX_TOTAL_TEXT_CHARS = 220_000;
    private static final List<String> RESEARCH_TERMS = List.of(
            "abstract", "introduction", "contribution", "method", "approach", "experiment",
            "evaluation", "dataset", "baseline", "metric", "result", "ablation", "limitation",
            "limitations", "future work", "conclusion", "complexity", "generalization"
    );

    public List<String> selectSnippets(String pdfText, String title, String tags) {
        String normalized = normalize(pdfText);
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() > MAX_TOTAL_TEXT_CHARS) {
            normalized = normalized.substring(0, MAX_TOTAL_TEXT_CHARS);
        }

        Set<String> queryTerms = new LinkedHashSet<>(RESEARCH_TERMS);
        addTerms(queryTerms, title);
        addTerms(queryTerms, tags);

        List<String> chunks = splitChunks(normalized);
        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, score(chunk, queryTerms)))
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .limit(MAX_SNIPPETS)
                .map(ScoredChunk::text)
                .map(this::trimSnippet)
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }

    private static void addTerms(Set<String> queryTerms, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() >= 4) {
                queryTerms.add(token);
            }
        }
    }

    private static List<String> splitChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end).strip());
            if (end == text.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private static int score(String chunk, Set<String> queryTerms) {
        String lower = chunk.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : queryTerms) {
            if (lower.contains(term)) {
                score += researchTermWeight(term);
            }
        }
        return score;
    }

    private static int researchTermWeight(String term) {
        return switch (term) {
            case "limitation", "limitations", "future work", "experiment", "evaluation", "result", "ablation" -> 5;
            case "dataset", "baseline", "metric", "contribution", "method", "approach" -> 3;
            default -> 1;
        };
    }

    private String trimSnippet(String value) {
        if (value.length() <= MAX_SNIPPET_CHARS) {
            return value;
        }
        return value.substring(0, MAX_SNIPPET_CHARS).strip();
    }

    private record ScoredChunk(String text, int score) {
    }
}
