package vn.edu.learningai.storage;

import org.junit.jupiter.api.Test;
import vn.edu.learningai.support.TestImages;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTypeTest {

    @Test
    void detectsPng() {
        assertThat(ImageType.detect(TestImages.png())).contains(ImageType.PNG);
    }

    @Test
    void detectsJpeg() {
        assertThat(ImageType.detect(TestImages.jpeg())).contains(ImageType.JPEG);
    }

    @Test
    void detectsWebp() {
        assertThat(ImageType.detect(TestImages.webp())).contains(ImageType.WEBP);
    }

    @Test
    void rejectsSvgEvenThoughItsBytesLookLikeMarkup() {
        assertThat(ImageType.detect(TestImages.svg())).isEmpty();
    }

    @Test
    void rejectsPlainTextAndEmptyContent() {
        assertThat(ImageType.detect("hello".getBytes())).isEmpty();
        assertThat(ImageType.detect(new byte[0])).isEmpty();
    }

    @Test
    void rejectsTruncatedSignatures() {
        assertThat(ImageType.detect(new byte[] {(byte) 0x89, 'P'})).isEmpty();
        assertThat(ImageType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8})).isEmpty();
        assertThat(ImageType.detect("RIFF".getBytes())).isEmpty();
    }

    @Test
    void riffWithoutWebpFormIsNotAnImage() {
        // A WAV file also starts with RIFF but must not pass.
        byte[] wav = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'A', 'V', 'E'};
        assertThat(ImageType.detect(wav)).isEqualTo(Optional.empty());
    }
}
