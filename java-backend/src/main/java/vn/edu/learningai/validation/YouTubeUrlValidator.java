package vn.edu.learningai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public class YouTubeUrlValidator implements ConstraintValidator<YouTubeUrl, String> {
    private static final Set<String> YOUTUBE_HOSTS = Set.of(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            host = host.toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (host.equals("youtu.be")) {
                return hasValueAfterPrefix(path, "/");
            }
            if (!YOUTUBE_HOSTS.contains(host)) {
                return false;
            }
            if (path.equals("/watch")) {
                UriComponents components = UriComponentsBuilder.fromUri(uri).build();
                String videoId = components.getQueryParams().getFirst("v");
                return videoId != null && !videoId.isBlank();
            }
            return hasValueAfterPrefix(path, "/embed/") || hasValueAfterPrefix(path, "/shorts/");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean hasValueAfterPrefix(String path, String prefix) {
        return path.startsWith(prefix) && path.substring(prefix.length()).trim().length() > 0;
    }
}
