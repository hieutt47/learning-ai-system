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

/**
 * Guest-facing course shape from contracts/payloads.md.
 *
 * <p>Deliberately has no {@code archived_at}, {@code is_published} or storage
 * key: the marketplace only exposes published, unarchived courses, so those
 * fields would either be constant or leak internals.</p>
 */
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

    /** ISO 4217 code; MVP prices are VND. */
    private String currency;

    /** Enrollments so far. */
    private long totalStudents;

    /**
     * Only present on the detail response; the list omits it so a page of
     * courses does not fan out into one outline query per row.
     */
    private List<OutlineItem> outline;

    /**
     * Number of lessons, present on list responses.
     *
     * <p>The list omits the outline (one query per row would be wasteful) but a
     * card still needs the count; sending 0 there would tell a guest a course
     * has no lessons when it may have ten.</p>
     */
    private Integer lessonCount;

    /**
     * Set when the course was archived after the caller enrolled.
     *
     * <p>Only meaningful in "my courses": the marketplace never returns archived
     * courses, so it stays null there. It exists so a student can be told why a
     * course is still listed but no longer in the catalogue.</p>
     */
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

    /**
     * A lesson as seen by a guest: no video URL, document list or transcript,
     * because those are for owners and enrolled students only (T08).
     */
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
