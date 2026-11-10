package vn.edu.learningai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.response.CoursePublicResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.service.MarketplaceService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class MarketplaceController {
    private final MarketplaceService marketplaceService;
    @GetMapping
    public ResponseEntity<PageResponse<CoursePublicResponse>> search(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "category_id", required = false) Long categoryId,
        @RequestParam(name = "sort", required = false) String sort,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(marketplaceService.search(query, categoryId, sort, page, pageSize));
    }
    @GetMapping("/{id}")
    public ResponseEntity<CoursePublicResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(marketplaceService.getPublicCourse(id));
    }
}
