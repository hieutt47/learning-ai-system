package vn.edu.learningai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_members")
@EntityListeners(AuditingEntityListener.class)
public class CourseMember {
    @EmbeddedId
    private CourseMemberId id;

    /**
     * Set by the writer, not by auditing.
     *
     * <p>{@code @CreatedDate} would overwrite it with a nanosecond clock value,
     * and the {@code DATETIME(6)} column keeps only microseconds, so the value
     * returned by the create call would differ from every later read of the
     * same row.</p>
     */
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;
}
