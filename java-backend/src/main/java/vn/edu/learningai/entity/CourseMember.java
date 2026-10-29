package vn.edu.learningai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
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

    @CreatedDate
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;
}
