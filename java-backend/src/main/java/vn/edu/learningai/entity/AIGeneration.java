package vn.edu.learningai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.edu.learningai.common.enums.Difficulty;
import vn.edu.learningai.common.enums.GenerationStatus;
import vn.edu.learningai.common.enums.SourceType;

import java.time.Instant;

/**
 * Bản ghi một lần sinh nội dung bằng AI (bảng ai_generations): provenance + candidate đã validate.
 * Lesson/document/user giữ dạng ID vì entity của module lesson/identity chưa có trên develop.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ai_generations")
@EntityListeners(AuditingEntityListener.class)
public class AIGeneration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false)
    private vn.edu.learningai.common.enums.GenerationType generationType;

    @Column(name = "requested_count", nullable = false)
    private Integer requestedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private Difficulty difficulty;

    @Column(name = "provider", length = 100, nullable = false)
    private String provider;

    @Column(name = "model", length = 100, nullable = false)
    private String model;

    @Column(name = "prompt_version", length = 50, nullable = false)
    private String promptVersion;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_hash", length = 64, nullable = false)
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GenerationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "candidate_json")
    private String candidateJson;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
