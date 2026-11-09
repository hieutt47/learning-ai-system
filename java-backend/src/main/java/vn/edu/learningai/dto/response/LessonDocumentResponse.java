package vn.edu.learningai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record LessonDocumentResponse(
    Long id,
    @JsonProperty("lesson_id") Long lessonId,
    @JsonProperty("original_name") String originalName,
    @JsonProperty("media_type") String mediaType,
    @JsonProperty("size_bytes") Long sizeBytes,
    @JsonProperty("text_extracted") boolean textExtracted,
    @JsonProperty("created_at") Instant createdAt
) {
}
