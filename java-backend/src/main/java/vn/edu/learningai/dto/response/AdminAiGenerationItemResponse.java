package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import vn.edu.learningai.common.enums.Difficulty;
import vn.edu.learningai.common.enums.GenerationStatus;
import vn.edu.learningai.common.enums.GenerationType;
import vn.edu.learningai.common.enums.SourceType;

import java.time.Instant;

/** Thông tin một lượt sinh AI kèm ngữ cảnh khóa học/bài học phục vụ Admin và giảng viên xem lại. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminAiGenerationItemResponse(
    String id,
    String lessonId,
    String lessonTitle,
    String courseId,
    String courseTitle,
    String sourceDocumentId,
    String sourceDocumentName,
    SourceType sourceType,
    GenerationType generationType,
    Integer requestedCount,
    Difficulty difficulty,
    String provider,
    String model,
    String promptVersion,
    GenerationStatus status,
    String candidateJson,
    String errorCode,
    String createdBy,
    String creatorName,
    Instant createdAt
) {
}

