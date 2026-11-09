package vn.edu.learningai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.dto.request.LessonReorderRequest;
import vn.edu.learningai.dto.request.LessonWriteRequest;
import vn.edu.learningai.dto.response.LessonDocumentResponse;
import vn.edu.learningai.dto.response.LessonResponse;
import vn.edu.learningai.service.CurrentUserProvider;
import vn.edu.learningai.service.LessonService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LessonController {
    private final LessonService lessonService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/courses/{courseId}/lessons")
    public List<LessonResponse> listLessons(@PathVariable Long courseId) {
        return lessonService.listForCourse(courseId, currentUserProvider.currentUserId());
    }

    @PostMapping("/courses/{courseId}/lessons")
    public ResponseEntity<LessonResponse> createLesson(
        @PathVariable Long courseId,
        @Valid @RequestBody LessonWriteRequest request
    ) {
        LessonResponse response = lessonService.create(courseId, request, currentUserProvider.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/courses/{courseId}/lessons/order")
    public List<LessonResponse> reorderLessons(
        @PathVariable Long courseId,
        @Valid @RequestBody LessonReorderRequest request
    ) {
        return lessonService.reorder(courseId, request, currentUserProvider.currentUserId());
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonResponse getLesson(@PathVariable Long lessonId) {
        return lessonService.get(lessonId, currentUserProvider.currentUserId());
    }

    @PutMapping("/lessons/{lessonId}")
    public LessonResponse updateLesson(
        @PathVariable Long lessonId,
        @Valid @RequestBody LessonWriteRequest request
    ) {
        return lessonService.update(lessonId, request, currentUserProvider.currentUserId());
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        lessonService.delete(lessonId, currentUserProvider.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lessons/{lessonId}/documents")
    public List<LessonDocumentResponse> listDocuments(@PathVariable Long lessonId) {
        return lessonService.listDocuments(lessonId, currentUserProvider.currentUserId());
    }

    @PostMapping(value = "/lessons/{lessonId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonDocumentResponse> uploadDocument(
        @PathVariable Long lessonId,
        @RequestParam("file") MultipartFile file
    ) {
        LessonDocumentResponse response = lessonService.uploadDocument(
            lessonId,
            file,
            currentUserProvider.currentUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/{documentId}/content")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        LessonService.DocumentDownload download = lessonService.downloadDocument(
            documentId,
            currentUserProvider.currentUserId()
        );
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(download.originalName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.mediaType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(download.resource());
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        lessonService.deleteDocument(documentId, currentUserProvider.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
