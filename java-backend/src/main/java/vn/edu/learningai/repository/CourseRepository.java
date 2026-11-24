package vn.edu.learningai.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.entity.Course;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    Page<Course> findByIsPublishedTrueAndArchivedAtIsNull(Pageable pageable);

    Page<Course> findByTeacherId(Long teacherId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByThumbnailStorageKey(String thumbnailStorageKey);

    @EntityGraph(attributePaths = {"category", "teacher"})
    Optional<Course> findWithCategoryAndTeacherById(Long id);
}
