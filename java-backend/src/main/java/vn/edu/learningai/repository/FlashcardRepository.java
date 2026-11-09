package vn.edu.learningai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.common.enums.FlashcardStatus;
import vn.edu.learningai.entity.Flashcard;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    Page<Flashcard> findByLessonId(Long lessonId, Pageable pageable);

    Page<Flashcard> findByLessonIdAndStatus(Long lessonId, FlashcardStatus status, Pageable pageable);
}
