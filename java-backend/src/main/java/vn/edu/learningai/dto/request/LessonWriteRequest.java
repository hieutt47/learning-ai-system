package vn.edu.learningai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonWriteRequest(
    @NotBlank @Size(max = 200) String title,
    @JsonProperty("video_url") @Size(max = 2048) String videoUrl,
    @JsonProperty("transcript_text") String transcriptText
) {
}
