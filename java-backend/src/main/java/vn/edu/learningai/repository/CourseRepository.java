package vn.edu.learningai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.Course;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findByIsPublishedTrueAndArchivedAtIsNull(Pageable pageable);

    Page<Course> findByTeacherId(Long teacherId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByThumbnailStorageKey(String thumbnailStorageKey);

    @EntityGraph(attributePaths = {"category", "teacher"})
    Optional<Course> findWithCategoryAndTeacherById(Long id);
}
