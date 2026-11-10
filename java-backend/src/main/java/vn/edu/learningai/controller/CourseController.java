package vn.edu.learningai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.dto.request.CourseArchiveRequest;
import vn.edu.learningai.dto.request.CoursePatchRequest;
import vn.edu.learningai.dto.request.CoursePublicationRequest;
import vn.edu.learningai.dto.request.CourseWriteRequest;
import vn.edu.learningai.dto.response.CourseOwnerResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.security.CurrentUserProvider;
import vn.edu.learningai.service.CoursePublicationService;
import vn.edu.learningai.service.CourseService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CourseController {
    private final CourseService courseService;
    private final CoursePublicationService coursePublicationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/courses")
    public ResponseEntity<CourseOwnerResponse> create(@Valid @RequestBody CourseWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(courseService.create(request, currentUserProvider.get()));
    }

    @PatchMapping("/courses/{id}")
    public ResponseEntity<CourseOwnerResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody CoursePatchRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(courseService.update(id, request, currentUserProvider.get()));
    }

    @GetMapping("/teaching/courses")
    public ResponseEntity<PageResponse<CourseOwnerResponse>> listForTeaching(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(courseService.listForTeaching(currentUserProvider.get(), page, pageSize));
    }

    @GetMapping("/teaching/courses/{id}")
    public ResponseEntity<CourseOwnerResponse> getOwned(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(courseService.getOwned(id, currentUserProvider.get()));
    }

    @PostMapping(path = "/courses/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseOwnerResponse> uploadThumbnail(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(courseService.updateThumbnail(id, file, currentUserProvider.get()));
    }

    @PatchMapping("/courses/{id}/publication")
    public ResponseEntity<CourseOwnerResponse> setPublication(
        @PathVariable Long id,
        @Valid @RequestBody CoursePublicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(coursePublicationService.setPublished(
            id, request.getIsPublished(), currentUserProvider.get()));
    }

    @PatchMapping("/courses/{id}/archive")
    public ResponseEntity<CourseOwnerResponse> archive(
        @PathVariable Long id,
        @Valid @RequestBody CourseArchiveRequest request
    ) {
        if (!Boolean.TRUE.equals(request.getArchived())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Only archived=true is supported");
        }
        return ResponseEntity.status(HttpStatus.OK)
            .body(coursePublicationService.archive(id, currentUserProvider.get()));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id, currentUserProvider.get());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
