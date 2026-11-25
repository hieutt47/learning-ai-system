package vn.edu.learningai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.common.enums.GenerationStatus;
import vn.edu.learningai.common.enums.GenerationType;
import vn.edu.learningai.dto.response.AdminAiGenerationItemResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.service.AdminAiService;

/** API chỉ đọc lịch sử sinh AI của bài học cho Admin và giảng viên sở hữu khóa học. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AdminAiController {

    private final AdminAiService adminAiService;

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @GetMapping("/lessons/{lessonId}/ai-generations")
    public PageResponse<AdminAiGenerationItemResponse> listLessonAiGenerations(
        @PathVariable Long lessonId,
        @RequestParam(name = "generation_type", required = false) GenerationType generationType,
        @RequestParam(name = "status", required = false) GenerationStatus status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        return adminAiService.listLessonAiGenerations(lessonId, generationType, status, page, pageSize);
    }
}
