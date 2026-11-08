package vn.edu.learningai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

/** Result of enrolling: the membership that now exists, whether new or pre-existing. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long courseId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long studentId;

    private Instant enrolledAt;
}
