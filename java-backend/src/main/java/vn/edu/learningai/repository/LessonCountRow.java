package vn.edu.learningai.repository;

/**
 * Projection interface tương thích cả JPQL lẫn Native SQL khi đếm theo lô GROUP BY lesson_id.
 */
public interface LessonCountRow {
    Long getLessonId();

    Long getTotal();
}
