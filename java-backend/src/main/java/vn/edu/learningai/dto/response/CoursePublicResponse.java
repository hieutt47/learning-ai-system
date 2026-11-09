package vn.edu.learningai.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePublicResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String description;
    private Category category;

    private Teacher teacher;

    private String thumbnailUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;

    private String currency;

    private long totalStudents;

    private List<OutlineItem> outline;

    private Integer lessonCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant archivedAt;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String name;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Teacher {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String fullName;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutlineItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String title;
        private Integer orderIndex;
    }
}
