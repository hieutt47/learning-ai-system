package vn.edu.learningai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuizContextRepository {
    private final JdbcClient jdbc;

    public Optional<LessonContext> findLesson(Long lessonId) {
        return jdbc.sql("""
            SELECT l.course_id, c.teacher_id
            FROM lessons l JOIN courses c ON c.id = l.course_id
            WHERE l.id = :lesson_id
            """)
            .param("lesson_id", lessonId)
            .query((rs, row) -> new LessonContext(rs.getLong("course_id"), rs.getLong("teacher_id")))
            .optional();
    }

    public boolean isEnrolled(Long courseId, Long studentId) {
        return jdbc.sql("""
            SELECT COUNT(*) FROM course_members
            WHERE course_id = :course_id AND student_id = :student_id
            """)
            .param("course_id", courseId)
            .param("student_id", studentId)
            .query(Long.class).single() > 0;
    }

    public Optional<GenerationContext> findGeneration(Long generationId) {
        return jdbc.sql("""
            SELECT lesson_id, created_by, generation_type, status
            FROM ai_generations WHERE id = :generation_id
            """)
            .param("generation_id", generationId)
            .query((rs, row) -> new GenerationContext(rs.getLong("lesson_id"), rs.getLong("created_by"),
                rs.getString("generation_type"), rs.getString("status")))
            .optional();
    }

    public record LessonContext(Long courseId, Long teacherId) {}

    public record GenerationContext(Long lessonId, Long createdBy, String type, String status) {}
}
