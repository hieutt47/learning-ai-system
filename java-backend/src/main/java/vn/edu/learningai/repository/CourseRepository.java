package vn.edu.learningai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findByIsPublishedTrueAndArchivedAtIsNull(Pageable pageable);
}
