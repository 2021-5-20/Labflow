package dev.labflow.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RagTextChunker {
    private static final int CHUNK_SIZE = 1400;
    private static final int HARD_CHUNK_SIZE = 1600;
    private static final int MAX_CHUNKS_PER_SOURCE = 80;
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\s*\\n+");
    private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?。！？])\\s+");

    public List<String> split(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> blocks = splitParagraphs(normalized);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : blocks) {
            if (block.length() > CHUNK_SIZE) {
                flush(current, chunks);
                chunks.addAll(splitLongBlock(block));
                if (chunks.size() >= MAX_CHUNKS_PER_SOURCE) {
                    return chunks.subList(0, MAX_CHUNKS_PER_SOURCE);
                }
                continue;
            }

            int projectedLength = current.isEmpty()
                    ? block.length()
                    : current.length() + 2 + block.length();
            if (projectedLength > CHUNK_SIZE) {
                flush(current, chunks);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(block);
        }
        flush(current, chunks);
        if (chunks.size() > MAX_CHUNKS_PER_SOURCE) {
            return chunks.subList(0, MAX_CHUNKS_PER_SOURCE);
        }
        return chunks;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String unixLines = value.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = Arrays.stream(unixLines.split("\n", -1))
                .map(line -> line.replaceAll("[\\t ]+", " ").strip())
                .toList();
        return String.join("\n", lines)
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static List<String> splitParagraphs(String text) {
        return PARAGRAPH_BREAK.splitAsStream(text)
                .map(String::strip)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static List<String> splitLongBlock(String block) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : SENTENCE_BREAK.split(block)) {
            String cleanSentence = sentence.strip();
            if (cleanSentence.isBlank()) {
                continue;
            }
            if (cleanSentence.length() > HARD_CHUNK_SIZE) {
                flush(current, chunks);
                chunks.addAll(splitOversizedSentence(cleanSentence));
                continue;
            }
            int projectedLength = current.isEmpty()
                    ? cleanSentence.length()
                    : current.length() + 1 + cleanSentence.length();
            if (projectedLength > CHUNK_SIZE) {
                flush(current, chunks);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(cleanSentence);
        }
        flush(current, chunks);
        return chunks;
    }

    private static List<String> splitOversizedSentence(String sentence) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < sentence.length()) {
            int end = Math.min(start + HARD_CHUNK_SIZE, sentence.length());
            int boundary = findBoundary(sentence, start, end);
            chunks.add(sentence.substring(start, boundary).strip());
            start = boundary;
        }
        return chunks.stream().filter(item -> !item.isBlank()).toList();
    }

    private static int findBoundary(String sentence, int start, int end) {
        if (end == sentence.length()) {
            return end;
        }
        for (int i = end; i > start + CHUNK_SIZE; i--) {
            if (Character.isWhitespace(sentence.charAt(i - 1))) {
                return i;
            }
        }
        return end;
    }

    private static void flush(StringBuilder current, List<String> chunks) {
        String value = current.toString().strip();
        if (!value.isBlank()) {
            chunks.add(value);
            current.setLength(0);
        }
    }
}
