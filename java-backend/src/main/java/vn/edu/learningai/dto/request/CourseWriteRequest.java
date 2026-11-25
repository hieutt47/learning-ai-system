package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CourseWriteRequest {
    @NotBlank(message = "Tên khóa học không được để trống")
    @Size(max = 200, message = "Tên khóa học tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Mô tả khóa học không được để trống")
    private String description;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotNull(message = "Vui lòng nhập giá khóa học")
    @DecimalMin(value = "0.00", message = "Giá khóa học phải từ 0 trở lên")
    @Digits(integer = 12, fraction = 2, message = "Giá khóa học không hợp lệ")
    private BigDecimal price;
}
