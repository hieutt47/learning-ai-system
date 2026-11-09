package vn.edu.learningai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.response.CourseEnrollmentResponse;
import vn.edu.learningai.dto.response.EnrollmentResponse;
import vn.edu.learningai.dto.response.MemberResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.security.CurrentUserProvider;
import vn.edu.learningai.service.EnrollmentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final CurrentUserProvider currentUserProvider;

    /** 201 for a new membership, 200 when the student already had one. */
    @PostMapping("/courses/{id}/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long id) {
        EnrollmentService.EnrollmentResult result =
            enrollmentService.enroll(id, currentUserProvider.get());

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping("/users/me/courses")
    public ResponseEntity<PageResponse<CourseEnrollmentResponse>> myCourses(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(enrollmentService.listMine(currentUserProvider.get(), page, pageSize));
    }

    @GetMapping("/courses/{id}/members")
    public ResponseEntity<PageResponse<MemberResponse>> members(
        @PathVariable Long id,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(enrollmentService.listMembers(id, currentUserProvider.get(), page, pageSize));
    }
}
