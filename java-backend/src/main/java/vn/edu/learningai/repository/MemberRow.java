package vn.edu.learningai.repository;

import java.time.Instant;

public record MemberRow(Long studentId, String fullName, Instant enrolledAt) {
}
