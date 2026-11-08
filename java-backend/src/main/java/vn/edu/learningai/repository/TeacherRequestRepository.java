package vn.edu.learningai.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import vn.edu.learningai.common.enums.TeacherRequestStatus;
import vn.edu.learningai.entity.TeacherRequest;

import java.util.Optional;

public interface TeacherRequestRepository extends JpaRepository<TeacherRequest, Long> {
    boolean existsByUserIdAndStatus(Long userId, TeacherRequestStatus status);

    @EntityGraph(attributePaths = {"user", "decidedBy"})
    Page<TeacherRequest> findByUserId(Long userId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "decidedBy"})
    Page<TeacherRequest> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "decidedBy"})
    Page<TeacherRequest> findByStatus(TeacherRequestStatus status, Pageable pageable);

}
