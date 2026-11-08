package vn.edu.learningai.storage;

import java.util.Optional;

/**
 * Image formats accepted for thumbnails (contract: JPEG/PNG/WebP only).
 *
 * <p>Detection reads the file's magic bytes instead of trusting the client
 * filename or {@code Content-Type}, because both are attacker controlled: a
 * text or SVG payload renamed to {@code logo.png} must be rejected, not stored
 * and later served back as HTML.</p>
 */
public enum ImageType {
    PNG("png"),
    JPEG("jpg"),
    WEBP("webp");

    /** Extension the server assigns; the client filename is never reused. */
    private final String extension;

    ImageType(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageType> detect(byte[] content) {
        return Optional.ofNullable(detectOrNull(content));
    }

    private static ImageType detectOrNull(byte[] b) {
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (b.length >= 8
            && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
            && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
            && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return PNG;
        }
        // JPEG: FF D8 FF
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        // WebP: "RIFF" <4-byte size> "WEBP"
        if (b.length >= 12
            && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
            && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return WEBP;
        }
        return null;
    }
}
