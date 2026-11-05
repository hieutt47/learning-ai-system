package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.AIGeneration;

public interface AiGenerationRepository extends JpaRepository<AIGeneration, Long> {
}
