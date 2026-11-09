package vn.edu.learningai.repository;

import java.time.Instant;

/**
 * A course member row as the owner sees it.
 *
 * <p>Selects only id, name and join time: email is never read from the
 * database for this route, so it cannot be added to a response by mistake.</p>
 */
public record MemberRow(Long studentId, String fullName, Instant enrolledAt) {
}
