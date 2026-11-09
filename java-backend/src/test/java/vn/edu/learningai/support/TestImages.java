package vn.edu.learningai.support;

import java.util.Arrays;

/**
 * Minimal byte sequences that carry real magic numbers, so type detection is
 * exercised the same way an upload would be. Not valid decodable images; the
 * server only inspects signatures, and a real encoder would not add coverage.
 */
public final class TestImages {
    private TestImages() {
    }

    public static byte[] png() {
        return concat(
            concat(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
                new byte[] {0, 0, 0, 13, 'I', 'H', 'D', 'R'}),
            zeros(32));
    }

    public static byte[] jpeg() {
        return concat(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, zeros(32));
    }

    public static byte[] webp() {
        return concat(new byte[] {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'}, zeros(32));
    }

    /** SVG is text, not a raster format: it must be rejected even when named .png. */
    public static byte[] svg() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes();
    }

    public static byte[] ofSize(int size) {
        byte[] content = concat(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, zeros(size - 8));
        return content;
    }

    private static byte[] zeros(int size) {
        return new byte[size];
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
