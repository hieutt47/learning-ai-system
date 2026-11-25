package vn.edu.learningai.service.lesson;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Chính sách kiểm tra và chuẩn hóa YouTube URL dùng chung cho Validator và Service.
 * Chặn HTML iframe, URL lạ, thiếu video ID hoặc path thừa; chuẩn hóa về dạng watch?v=<id>.
 */
@Component
public class YouTubeUrlPolicy {
    private static final Set<String> YOUTUBE_HOSTS = Set.of(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com"
    );
    private static final Set<String> SHORT_HOSTS = Set.of(
        "youtu.be",
        "www.youtu.be"
    );
    private static final Set<String> NOCOOKIE_HOSTS = Set.of(
        "youtube-nocookie.com",
        "www.youtube-nocookie.com"
    );
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{6,64}$");

    public static boolean isValidOptional(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return extractVideoId(value).isPresent();
    }

    public static Optional<String> extractVideoId(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return Optional.empty();
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();

            String candidate = null;
            if (SHORT_HOSTS.contains(normalizedHost)) {
                candidate = singleSegmentAfter(path, "/");
            } else if (NOCOOKIE_HOSTS.contains(normalizedHost)) {
                candidate = singleSegmentAfter(path, "/embed/");
            } else if (YOUTUBE_HOSTS.contains(normalizedHost)) {
                if (path.equals("/watch")) {
                    UriComponents components = UriComponentsBuilder.fromUri(uri).build();
                    candidate = components.getQueryParams().getFirst("v");
                } else if (path.startsWith("/embed/")) {
                    candidate = singleSegmentAfter(path, "/embed/");
                } else if (path.startsWith("/shorts/")) {
                    candidate = singleSegmentAfter(path, "/shorts/");
                }
            }

            if (candidate == null) {
                return Optional.empty();
            }
            String id = candidate.trim();
            return VIDEO_ID_PATTERN.matcher(id).matches() ? Optional.of(id) : Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String videoId = extractVideoId(value)
            .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid YouTube URL"));
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private static String singleSegmentAfter(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return null;
        }
        String rest = path.substring(prefix.length());
        if (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        if (rest.isEmpty() || rest.contains("/")) {
            return null;
        }
        return rest;
    }
}
