package vn.edu.learningai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    private List<T> items;
    private Integer page;
    private Integer pageSize;
    private Long total;

    public static <T> PageResponse<T> from(Page<T> result) {
        return PageResponse.<T>builder()
            .items(result.getContent())
            .page(result.getNumber())
            .pageSize(result.getSize())
            .total(result.getTotalElements())
            .build();
    }
}
