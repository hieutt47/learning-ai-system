package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.common.enums.Difficulty;
import vn.edu.learningai.common.enums.GenerationStatus;
import vn.edu.learningai.common.enums.GenerationType;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.response.AdminAiGenerationItemResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.security.SecurityHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Truy vấn chỉ đọc lịch sử sinh AI của một bài học cho Admin và giảng viên sở hữu khóa học. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiService {

    private final JdbcClient jdbc;
    private final SecurityHelper security;

    public PageResponse<AdminAiGenerationItemResponse> listLessonAiGenerations(
        Long lessonId,
        GenerationType generationType,
        GenerationStatus status,
        int page,
        int pageSize
    ) {
        validatePagination(page, pageSize);
        if (lessonId == null) {
            throw ApiException.fieldError("lesson_id", "lesson_id is required");
        }
        authorizeLessonRead(lessonId);

        Map<String, Object> params = new LinkedHashMap<>();
        List<String> where = new ArrayList<>();
        where.add("g.lesson_id = :lessonId");
        params.put("lessonId", lessonId);
        if (generationType != null) {
            where.add("g.generation_type = :generationType");
            params.put("generationType", generationType.name());
        }
        if (status != null) {
            where.add("g.status = :status");
            params.put("status", status.name());
        }

        String whereClause = " WHERE " + String.join(" AND ", where);
        String baseFrom = """
            FROM ai_generations g
            JOIN lessons l ON l.id = g.lesson_id
            JOIN courses c ON c.id = l.course_id
            JOIN users u ON u.id = g.created_by
            LEFT JOIN lesson_documents d ON d.id = g.source_document_id
            """ + whereClause;

        long total = jdbc.sql("SELECT COUNT(*) " + baseFrom)
            .params(params)
            .query(Long.class)
            .single();

        params.put("limit", pageSize);
        params.put("offset", (page - 1) * pageSize);

        List<AdminAiGenerationItemResponse> items = jdbc.sql("""
            SELECT g.id, g.lesson_id, l.title AS lesson_title, c.id AS course_id, c.title AS course_title,
                   g.source_document_id, d.original_name AS source_document_name,
                   g.source_type, g.generation_type, g.requested_count, g.difficulty,
                   g.provider, g.model, g.prompt_version, g.status, g.candidate_json, g.error_code,
                   g.created_by, u.full_name AS creator_name, g.created_at
            """ + baseFrom + " ORDER BY g.created_at DESC, g.id DESC LIMIT :limit OFFSET :offset")
            .params(params)
            .query(this::mapGeneration)
            .list();

        return PageResponse.<AdminAiGenerationItemResponse>builder()
            .items(items)
            .page(page)
            .pageSize(pageSize)
            .total(total)
            .build();
    }

    private void authorizeLessonRead(Long lessonId) {
        User actor = security.getCurrentUser();
        Long ownerId = jdbc.sql("""
            SELECT c.teacher_id
            FROM lessons l
            JOIN courses c ON c.id = l.course_id
            WHERE l.id = :lessonId
            """)
            .param("lessonId", lessonId)
            .query(Long.class)
            .optional()
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
        if (actor.getRole() != UserRole.ADMIN
            && (actor.getRole() != UserRole.TEACHER || !actor.getId().equals(ownerId))) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 1) {
            throw ApiException.fieldError("page", "page must be >= 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw ApiException.fieldError("page_size", "page_size must be between 1 and 100");
        }
    }

    private AdminAiGenerationItemResponse mapGeneration(ResultSet rs, int rowNum) throws SQLException {
        String diffRaw = rs.getString("difficulty");
        return new AdminAiGenerationItemResponse(
            String.valueOf(rs.getLong("id")),
            String.valueOf(rs.getLong("lesson_id")),
            rs.getString("lesson_title"),
            String.valueOf(rs.getLong("course_id")),
            rs.getString("course_title"),
            nullableId(rs, "source_document_id"),
            rs.getString("source_document_name"),
            SourceType.valueOf(rs.getString("source_type")),
            GenerationType.valueOf(rs.getString("generation_type")),
            rs.getInt("requested_count"),
            diffRaw == null ? null : Difficulty.valueOf(diffRaw),
            rs.getString("provider"),
            rs.getString("model"),
            rs.getString("prompt_version"),
            GenerationStatus.valueOf(rs.getString("status")),
            rs.getString("candidate_json"),
            rs.getString("error_code"),
            String.valueOf(rs.getLong("created_by")),
            rs.getString("creator_name"),
            toInstant(rs.getTimestamp("created_at"))
        );
    }

    private static String nullableId(ResultSet rs, String col) throws SQLException {
        long val = rs.getLong(col);
        return rs.wasNull() ? null : String.valueOf(val);
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
