package vn.edu.learningai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> items;
    private Integer page;
    @JsonProperty("page_size")
    private Integer pageSize;
    private Long total;

    public static <T> PageResponse<T> from(Page<T> result) {
        return PageResponse.<T>builder()
            .items(result.getContent())
            .page(result.getNumber() + 1)
            .pageSize(result.getSize())
            .total(result.getTotalElements())
            .build();
    }
    public static <T> PageResponse<T> from(Page<T> result, int requestedPage, int requestedPageSize) {
        return PageResponse.<T>builder()
            .items(result.getContent())
            .page(requestedPage)
            .pageSize(requestedPageSize)
            .total(result.getTotalElements())
            .build();
    }
}
