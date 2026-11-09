package vn.edu.learningai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

/**
 * A course member as the owner sees them.
 *
 * <p>Deliberately has no email or any other contact field: the owner needs to
 * know who is learning, not to harvest addresses (contracts/payloads.md).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long courseId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long studentId;

    private String fullName;

    private Instant enrolledAt;
}
