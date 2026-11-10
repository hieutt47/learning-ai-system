package vn.edu.learningai.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.edu.learningai.entity.Course;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EnrollmentQueryRepository {
    private final EntityManager entityManager;
    public Page<EnrolledCourse> findEnrolledCourses(Long studentId, Pageable pageable) {
        long total = entityManager.createQuery(
                "SELECT COUNT(m) FROM CourseMember m WHERE m.id.studentId = :studentId", Long.class)
            .setParameter("studentId", studentId)
            .getSingleResult();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        TypedQuery<Object[]> query = entityManager.createQuery(
                "SELECT c, m.enrolledAt FROM CourseMember m JOIN Course c ON c.id = m.id.courseId "
                    + "JOIN FETCH c.category JOIN FETCH c.teacher "
                    + "WHERE m.id.studentId = :studentId "
                    + "ORDER BY m.enrolledAt DESC, c.id DESC", Object[].class)
            .setParameter("studentId", studentId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize());
        List<EnrolledCourse> rows = query.getResultList().stream()
            .map(row -> new EnrolledCourse((Course) row[0], (Instant) row[1]))
            .toList();
        return new PageImpl<>(rows, pageable, total);
    }
    public long countEnrolledStudents(Long courseId) {
        return entityManager.createQuery(
                "SELECT COUNT(m) FROM CourseMember m WHERE m.id.courseId = :courseId", Long.class)
            .setParameter("courseId", courseId)
            .getSingleResult();
    }
    public java.util.Map<Long, Long> countEnrolledStudents(List<Long> courseIds) {
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        if (courseIds.isEmpty()) {
            return counts;
        }
        entityManager.createQuery(
                "SELECT m.id.courseId, COUNT(m) FROM CourseMember m "
                    + "WHERE m.id.courseId IN :courseIds GROUP BY m.id.courseId", Object[].class)
            .setParameter("courseIds", courseIds)
            .getResultList()
            .forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        return counts;
    }
    public Page<MemberRow> findMembers(Long courseId, Pageable pageable) {
        long total = entityManager.createQuery(
                "SELECT COUNT(m) FROM CourseMember m WHERE m.id.courseId = :courseId", Long.class)
            .setParameter("courseId", courseId)
            .getSingleResult();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        List<MemberRow> rows = entityManager.createQuery(
                "SELECT m.id.studentId, u.fullName, m.enrolledAt FROM CourseMember m "
                    + "JOIN User u ON u.id = m.id.studentId "
                    + "WHERE m.id.courseId = :courseId "
                    + "ORDER BY m.enrolledAt ASC, m.id.studentId ASC", Object[].class)
            .setParameter("courseId", courseId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList().stream()
            .map(row -> new MemberRow((Long) row[0], (String) row[1], (Instant) row[2]))
            .toList();
        return new PageImpl<>(rows, pageable, total);
    }
    public record EnrolledCourse(Course course, Instant enrolledAt) {
    }
}
