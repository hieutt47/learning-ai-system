package vn.edu.learningai.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.entity.CourseMember;
import vn.edu.learningai.entity.CourseMemberId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Repository
@RequiredArgsConstructor
public class EnrollmentWriter {
    private final EntityManager entityManager;
    @Transactional
    public void create(CourseMemberId id) {
        CourseMember member = CourseMember.builder()
            .id(id)
            .enrolledAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
            .build();
        entityManager.persist(member);
        entityManager.flush();
    }
}
