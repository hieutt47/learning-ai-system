package vn.edu.learningai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record LessonResponse(
    Long id,
    @JsonProperty("course_id") Long courseId,
    String title,
    @JsonProperty("video_url") String videoUrl,
    @JsonProperty("transcript_text") String transcriptText,
    @JsonProperty("order_index") Integer orderIndex,
    @JsonProperty("document_count") long documentCount,
    @JsonProperty("quiz_count") long quizCount,
    @JsonProperty("flashcard_count") long flashcardCount,
    @JsonProperty("can_delete") boolean canDelete,
    @JsonProperty("delete_blocked_reason") String deleteBlockedReason,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt
) {
}
