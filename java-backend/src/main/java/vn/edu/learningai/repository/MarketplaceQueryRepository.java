package vn.edu.learningai.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import vn.edu.learningai.entity.Course;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Marketplace reads that derived queries cannot express: an optional keyword
 * and an optional category filter in one query, plus per-course counts.
 *
 * <p>Filters are bound as parameters; the keyword only ever appears inside a
 * {@code LIKE} value, never in the query text. Sorting is a whitelist because
 * a client-supplied property name is a JPQL injection point.</p>
 */
@Repository
@RequiredArgsConstructor
public class MarketplaceQueryRepository {
    private static final String PUBLISHED_ONLY = "c.isPublished = true AND c.archivedAt IS NULL";

    private final EntityManager entityManager;

    public Page<Course> searchPublished(String query, Long categoryId, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE ").append(PUBLISHED_ONLY);
        Map<String, Object> params = new HashMap<>();

        if (categoryId != null) {
            where.append(" AND c.category.id = :categoryId");
            params.put("categoryId", categoryId);
        }
        if (query != null && !query.isBlank()) {
            // '!' rather than '\' because an HQL string literal cannot hold a
            // single backslash: ESCAPE '\\' is parsed as two characters.
            where.append(" AND LOWER(c.title) LIKE :pattern ESCAPE '!'");
            params.put("pattern", "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%");
        }

        // Count first: when the page is past the end there is no need to select rows.
        TypedQuery<Long> countQuery = entityManager
            .createQuery("SELECT COUNT(c) FROM Course c" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Category and teacher are fetched with the page: both are many-to-one,
        // so this stays one row per course and saves two queries per result.
        TypedQuery<Course> selectQuery = entityManager
            .createQuery("SELECT c FROM Course c JOIN FETCH c.category JOIN FETCH c.teacher"
                + where + orderBy(pageable.getSort()), Course.class)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize());
        params.forEach(selectQuery::setParameter);

        return new PageImpl<>(selectQuery.getResultList(), pageable, total);
    }

    /**
     * Lessons as seen by a guest: no video URL or transcript is selected at all.
     *
     * <p>Built for the record's canonical constructor; Hibernate cannot package
     * a select list into a record/interface without one.</p>
     */
    public List<LessonOutlineRow> findOutline(Long courseId) {
        return entityManager.createQuery(
                // Lesson maps course_id as a plain column (lesson module owns the
                // entity), so this filters on courseId rather than a relation.
                "SELECT l.id, l.title, l.orderIndex FROM Lesson l "
                    + "WHERE l.courseId = :courseId ORDER BY l.orderIndex ASC", Object[].class)
            .setParameter("courseId", courseId)
            .getResultList().stream()
            .map(row -> new LessonOutlineRow((Long) row[0], (String) row[1], (Integer) row[2]))
            .toList();
    }

    public Map<Long, Long> countEnrollments(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createQuery(
                "SELECT m.id.courseId, COUNT(m) FROM CourseMember m "
                    + "WHERE m.id.courseId IN :courseIds GROUP BY m.id.courseId", Object[].class)
            .setParameter("courseIds", courseIds)
            .getResultList();

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    public long countEnrollments(Long courseId) {
        return entityManager.createQuery(
                "SELECT COUNT(m) FROM CourseMember m WHERE m.id.courseId = :courseId", Long.class)
            .setParameter("courseId", courseId)
            .getSingleResult();
    }

    private String orderBy(Sort sort) {
        Sort.Order order = sort.stream().findFirst().orElse(null);
        String property = order == null ? "createdAt" : order.getProperty();
        String direction = order == null || order.isAscending() ? "ASC" : "DESC";

        String column = switch (property) {
            case "price" -> "c.price";
            case "title" -> "c.title";
            case "updatedAt" -> "c.updatedAt";
            // Anything unrecognised falls back to the contract default rather
            // than being rejected, so a typo never becomes a 500.
            default -> "c.createdAt";
        };
        // Tie-break on id so equal values do not shuffle between pages.
        return " ORDER BY " + column + " " + direction + ", c.id DESC";
    }

    /**
     * Escapes the LIKE wildcards so a search for "%" does not match every
     * course. The escape character itself must be escaped first, otherwise a
     * search for "!" would produce an invalid pattern.
     */
    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
