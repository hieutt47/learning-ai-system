package vn.edu.learningai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * The pagination envelope from contracts/api-conventions.md:
 * {@code {items, page, page_size, total}} with page starting at 1.
 *
 * <p>{@code page_size} is annotated rather than relying on the global
 * snake_case strategy, so the wire shape holds even if that strategy is ever
 * changed or a test serializes without it.</p>
 */
@Getter
@Builder
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> items;

    private Integer page;

    @JsonProperty("page_size")
    private Integer pageSize;

    private Long total;

    /**
     * For callers that let Spring Data page the collection. The page number is
     * converted to the 1-based value the contract uses.
     */
    public static <T> PageResponse<T> from(Page<T> result) {
        return PageResponse.<T>builder()
            .items(result.getContent())
            .page(result.getNumber() + 1)
            .pageSize(result.getSize())
            .total(result.getTotalElements())
            .build();
    }

    /**
     * For services that validate the requested page and size themselves: echoes
     * what the caller asked for even when the page is past the end, which is
     * what makes {@code page} in the response meaningful.
     */
    public static <T> PageResponse<T> from(Page<T> result, int requestedPage, int requestedPageSize) {
        return PageResponse.<T>builder()
            .items(result.getContent())
            .page(requestedPage)
            .pageSize(requestedPageSize)
            .total(result.getTotalElements())
            .build();
    }
}
