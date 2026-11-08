package vn.edu.learningai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.common.enums.TeacherRequestStatus;
import vn.edu.learningai.dto.request.CreateTeacherRequest;
import vn.edu.learningai.dto.request.TeacherDecisionRequest;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.dto.response.TeacherRequestResponse;
import vn.edu.learningai.service.TeacherRequestService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher-requests")
public class TeacherRequestController {
    private final TeacherRequestService teacherRequestService;

    @PostMapping
    public ResponseEntity<TeacherRequestResponse> create(@Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(teacherRequestService.create(request));
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<TeacherRequestResponse>> getMyRequests(Pageable pageable) {
        return ResponseEntity.ok(teacherRequestService.getMyRequests(pageable));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TeacherRequestResponse>> getRequests(
        @RequestParam(name = "status", required = false) TeacherRequestStatus status,
        Pageable pageable) {
        return ResponseEntity.ok(teacherRequestService.getRequests(status, pageable));
    }

    @PatchMapping("/{request_id}/decision")
    public ResponseEntity<TeacherRequestResponse> decide(
        @PathVariable("request_id") Long requestId,
        @Valid @RequestBody TeacherDecisionRequest request) {
        return ResponseEntity.ok(teacherRequestService.decide(requestId, request));
    }
}
