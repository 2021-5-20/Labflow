package dev.labflow.file;

import java.io.InputStream;

public record FileContent(InputStream stream, String filename, String contentType, Long sizeBytes) {
}
