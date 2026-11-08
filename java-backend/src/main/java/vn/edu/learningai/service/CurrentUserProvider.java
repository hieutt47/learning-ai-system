package vn.edu.learningai.service;

/**
 * Lesson-module view of the current caller, as a user id only.
 *
 * <p>The security module already resolves the caller to an
 * {@link vn.edu.learningai.security.AuthenticatedUser} and checks that the
 * account exists and is active. This interface stays because the lesson
 * services only need an id; {@link LessonCurrentUserProvider} adapts one to
 * the other so there is a single source of truth for identity.</p>
 */
public interface CurrentUserProvider {
    Long currentUserId();
}
