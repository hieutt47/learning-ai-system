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
    private String teacherName;
    private String thumbnailUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;
    private Boolean isPublished;

    private Long lessonCount;

    private Long totalStudents;
    private Instant archivedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
