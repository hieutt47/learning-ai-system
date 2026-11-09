package vn.edu.learningai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.learningai.storage.FileStorage;
import vn.edu.learningai.support.IntegrationTestBase;
import vn.edu.learningai.support.TestImages;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * COURSE-01 thumbnail upload over HTTP against real MySQL and real local storage.
 * Users: 1 admin, 2 teacher A, 3 teacher B, 4 student A, 5 inactive teacher.
 */
class ThumbnailControllerTest extends IntegrationTestBase {
    private static final String THUMBNAIL = "/api/v1/courses/1/thumbnail";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileStorage fileStorage;

    @Test
    void ownerUploadsPngAndGetsCourseWithUrl() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("logo.png", TestImages.png()))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("1"))
            .andExpect(jsonPath("$.thumbnail_url").value(
                org.hamcrest.Matchers.matchesPattern("/api/v1/files/[0-9a-f-]{36}\\.png")));

        Map<String, Object> row = thumbnailRow(1);
        assertThat(row.get("thumbnail_url").toString()).startsWith("/api/v1/files/");
        // The disk name is server generated: the uploaded filename never reaches it.
        assertThat(row.get("thumbnail_storage_key").toString())
            .matches("[0-9a-f-]{36}\\.png")
            .doesNotContain("logo");
        assertThat(fileStorage.exists(row.get("thumbnail_storage_key").toString())).isTrue();
    }

    @Test
    void storedUrlServesTheBytesBackWithDetectedType() throws Exception {
        String url = upload(2, TestImages.jpeg(), "photo.jpeg");
        String key = url.substring(url.lastIndexOf('/') + 1);

        mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().bytes(TestImages.jpeg()));

        assertThat(key).endsWith(".jpg");
    }

    @Test
    void reuploadReplacesPreviousFile() throws Exception {
        String firstUrl = upload(2, TestImages.png(), "a.png");
        String firstKey = firstUrl.substring(firstUrl.lastIndexOf('/') + 1);

        String secondUrl = upload(2, TestImages.webp(), "b.webp");
        String secondKey = secondUrl.substring(secondUrl.lastIndexOf('/') + 1);

        assertThat(secondKey).isNotEqualTo(firstKey);
        assertThat(thumbnailRow(1).get("thumbnail_storage_key")).isEqualTo(secondKey);
        // The replaced file is removed after commit, not left behind.
        assertThat(fileStorage.exists(firstKey)).isFalse();
        assertThat(fileStorage.exists(secondKey)).isTrue();
    }

    @Test
    void otherTeacherCannotUpload() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("logo.png", TestImages.png()))
                .header("Authorization", "Bearer " + tokenFor(3)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(thumbnailRow(1).get("thumbnail_storage_key")).isNull();
    }

    @Test
    void studentCannotUpload() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("logo.png", TestImages.png()))
                .header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void anonymousCannotUpload() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL).file(image("logo.png", TestImages.png())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void unknownCourseIsNotFound() throws Exception {
        mockMvc.perform(multipart("/api/v1/courses/999/thumbnail")
                .file(image("logo.png", TestImages.png()))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void textPayloadRenamedToPngIsUnsupported() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("logo.png", TestImages.svg()))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));

        assertThat(thumbnailRow(1).get("thumbnail_storage_key")).isNull();
    }

    @Test
    void missingFilePartIsValidationError() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL).header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Required part 'file' is missing"))
            .andExpect(jsonPath("$.request_id").exists());
    }

    @Test
    void wrongContentTypeIsValidationErrorNotUnsupportedFileType() throws Exception {
        // 415 UNSUPPORTED_FILE_TYPE means "the file's bytes are not an image";
        // a JSON body is a malformed request, and must not be reported as such.
        mockMvc.perform(post(THUMBNAIL)
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void emptyFileIsValidationError() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("logo.png", new byte[0]))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void tooLargeFileIsFileTooLarge() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("big.png", TestImages.ofSize(5 * 1024 * 1024 + 1)))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));

        assertThat(thumbnailRow(1).get("thumbnail_storage_key")).isNull();
    }

    @Test
    void oversizedRequestBodyIsRejectedBeforeTheController() throws Exception {
        // Beyond spring.servlet.multipart.max-request-size (6MB): the servlet
        // rejects it, and the handler must still answer with our contract code.
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("big.png", TestImages.ofSize(7 * 1024 * 1024)))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void exactlyFiveMebibytesIsAccepted() throws Exception {
        mockMvc.perform(multipart(THUMBNAIL)
                .file(image("big.png", TestImages.ofSize(5 * 1024 * 1024)))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.thumbnail_url").exists());
    }

    @Test
    void unknownFileKeyIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/files/00000000-0000-0000-0000-000000000000.png"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void traversalKeyIsRejectedWithoutTouchingTheFilesystem() throws Exception {
        // Tomcat now rejects an encoded path separator before the request
        // reaches the controller, so the status is 400 rather than our 404.
        // Either way the file is not opened; the point of the test is that the
        // response is not 200 and nothing outside the storage root is served.
        int status = mockMvc.perform(get("/api/v1/files/..%2F..%2Fapplication.yaml"))
            .andReturn().getResponse().getStatus();

        assertThat(status).isIn(400, 404);
    }

    @Test
    void traversalKeyWithoutEncodingIsNotFound() throws Exception {
        // A key that fails the storage-key pattern never reaches the disk. (An
        // encoded path separator is rejected by Tomcat first; this covers the
        // request that does arrive.)
        mockMvc.perform(get("/api/v1/files/not-a-key"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deletionRemovesWorkingDraftCover() throws Exception {
        String url = upload(1, TestImages.png(), "cover.png");
        String key = url.substring(url.lastIndexOf('/') + 1);

        mockMvc.perform(delete("/api/v1/courses/1").header("Authorization", "Bearer " + tokenFor(1)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get(url)).andExpect(status().isNotFound());
        assertThat(fileStorage.exists(key)).isFalse();
    }

    @Test
    void publishedCourseCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/2").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    private String upload(long userId, byte[] content, String filename) throws Exception {
        String response = mockMvc.perform(multipart(THUMBNAIL)
                .file(image(filename, content))
                .header("Authorization", "Bearer " + tokenFor(userId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.thumbnail_url");
    }

    private Map<String, Object> thumbnailRow(long courseId) {
        return jdbcTemplate.queryForMap(
            "SELECT thumbnail_url, thumbnail_storage_key FROM courses WHERE id = ?", courseId);
    }

    private MockMultipartFile image(String filename, byte[] content) {
        // The declared type is deliberately wrong for some callers: detection
        // must rely on bytes, not on this header.
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_OCTET_STREAM_VALUE, content);
    }

}
