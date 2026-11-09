package vn.edu.learningai.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.entity.CourseMember;
import vn.edu.learningai.entity.CourseMemberId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Inserts a membership in a transaction of its own.
 *
 * <p>Separate from {@link EnrollmentService} because of how a duplicate key
 * marks a transaction: once the insert violates the composite primary key,
 * Spring flags the surrounding transaction rollback-only, so catching the
 * exception and carrying on inside the same transaction fails at commit with
 * {@code UnexpectedRollbackException}. Letting the failure end this
 * transaction lets the caller recover in a fresh one.</p>
 *
 * <p>Annotated {@link Repository} rather than {@code @Component} so Spring's
 * persistence-exception translation applies: without it a duplicate key
 * surfaces as Hibernate's {@code ConstraintViolationException}, which callers
 * catching {@code DataIntegrityViolationException} would miss.</p>
 */
@Repository
@RequiredArgsConstructor
public class EnrollmentWriter {
    private final EntityManager entityManager;

    /**
     * Inserts with {@code persist}, not {@code save}.
     *
     * <p>This entity has an assigned {@code @EmbeddedId}, so Spring Data's
     * {@code save} treats any non-null id as an existing row and issues a
     * {@code merge} — which, for a row that does not exist yet, quietly INSERTs,
     * and for a concurrent duplicate quietly UPDATEs. Either way no constraint
     * is violated and both callers believe they created the membership.
     * {@code persist} always INSERTs, so the primary key does its job.</p>
     *
     * <p>{@code enrolled_at} is truncated to microseconds before it is stored.
     * The column is {@code DATETIME(6)} and rounds anything finer, so a
     * nanosecond value set by auditing would change the first time the row is
     * read back — the create response and a later read would disagree.</p>
     *
     * @throws org.springframework.dao.DataIntegrityViolationException when the
     *         membership already exists, which is the expected outcome of a
     *         concurrent first request
     */
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
