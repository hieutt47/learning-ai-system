package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.learningai.common.enums.TeacherRequestStatus;

@Data
@NoArgsConstructor
public class TeacherDecisionRequest {
    @NotNull(message = "Decision status must not be null")
    private TeacherRequestStatus status;

    @Size(max = 4000, message = "Decision note must not exceed 4000 characters")
    private String decisionNote;
}
