package vn.edu.learningai.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BIGINT ids and decimal price are serialized as strings per contracts/api-conventions.md.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOwnerResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    private String categoryName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teacherId;

    /**
     * Owner's display name.
     *
     * <p>Present for the admin course list: showing a bare id there is useless,
     * and the admin list is the only screen that sees other people's courses by
     * id. Teacher-facing responses include it too; it is their own name.</p>
     */
    private String teacherName;

    private String thumbnailUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;

    private Boolean isPublished;

    /**
     * Counts shown on the teacher's course list and the publication checklist.
     * Served here rather than fetched per row by the client, which would turn a
     * page of ten courses into twenty extra requests.
     */
    private Long lessonCount;

    private Long totalStudents;

    private Instant archivedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
