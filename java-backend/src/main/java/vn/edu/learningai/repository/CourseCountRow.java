package vn.edu.learningai.repository;

/**
 * One row of a grouped count by course id.
 *
 * <p>A record rather than an interface projection: Hibernate packages a select
 * list into the canonical constructor, which it cannot do for an interface.</p>
 */
public record CourseCountRow(Long courseId, Long total) {
}
