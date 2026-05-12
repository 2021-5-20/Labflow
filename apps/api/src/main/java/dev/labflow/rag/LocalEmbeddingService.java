package dev.labflow.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LocalEmbeddingService {
    public static final int DIMENSION = 96;
    public static final String MODEL_NAME = "labflow-local-hash-v1";
    private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public LocalEmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public double[] embed(String text) {
        double[] vector = new double[DIMENSION];
        for (String token : tokenize(text)) {
            byte[] digest = sha256(token);
            int index = Byte.toUnsignedInt(digest[0]) % DIMENSION;
            double sign = (digest[1] & 1) == 0 ? 1.0 : -1.0;
            double weight = Math.min(2.5, 1.0 + Math.log10(token.length()));
            vector[index] += sign * weight;
        }
        normalize(vector);
        return vector;
    }

    public String toJson(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize local embedding", ex);
        }
    }

    public double[] fromJson(String json) {
        try {
            List<Double> values = objectMapper.readValue(json, DOUBLE_LIST);
            double[] vector = new double[DIMENSION];
            for (int i = 0; i < Math.min(values.size(), DIMENSION); i++) {
                vector[i] = values.get(i);
            }
            return vector;
        } catch (JsonProcessingException ex) {
            return new double[DIMENSION];
        }
    }

    public double cosine(double[] left, double[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static List<String> tokenize(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split("[^\\p{IsHan}a-z0-9]+")) {
            if (token.length() >= 2) {
                tokens.add(token);
                addNgrams(token, tokens);
            }
        }
        return tokens;
    }

    private static void addNgrams(String token, List<String> tokens) {
        if (token.length() < 5) {
            return;
        }
        for (int i = 0; i + 4 <= token.length(); i += 2) {
            tokens.add(token.substring(i, i + 4));
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static void normalize(double[] vector) {
        double norm = 0;
        for (double item : vector) {
            norm += item * item;
        }
        if (norm == 0) {
            return;
        }
        double root = Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / root;
        }
    }
}
