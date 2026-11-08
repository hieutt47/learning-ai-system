package vn.edu.learningai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.learningai.common.enums.TeacherRequestStatus;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherRequestResponse {
    private String id;
    private String userId;
    private String reason;
    private TeacherRequestStatus status;
    private String decidedBy;
    private String decisionNote;
    private Instant decidedAt;
    private Instant createdAt;
}
