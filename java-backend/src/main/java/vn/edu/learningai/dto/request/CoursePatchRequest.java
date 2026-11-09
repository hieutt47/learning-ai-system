package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePatchRequest {
    @Size(max = 200)
    private String title;
    private String description;
    private Long categoryId;
    @DecimalMin(value = "0.00")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal price;
}
