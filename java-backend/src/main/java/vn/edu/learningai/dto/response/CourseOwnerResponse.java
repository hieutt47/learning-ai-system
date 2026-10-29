package vn.edu.learningai.dto.response;

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
    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long teacherId;
    private String thumbnailUrl;
    private BigDecimal price;
    private Boolean isPublished;
    private Instant archivedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
