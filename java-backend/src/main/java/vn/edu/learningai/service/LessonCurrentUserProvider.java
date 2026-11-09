package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.learningai.security.AuthenticatedUser;
import vn.edu.learningai.security.CurrentUserProvider;

/**
 * Adapts the security module's current-user resolution to the id-only shape the
 * lesson services expect.
 *
 * <p>Delegates rather than reading the header itself, so lesson routes get the
 * same checks as course and enrollment routes: the header is parsed once, the
 * user must exist, and an inactive account is rejected with ACCOUNT_INACTIVE.</p>
 */
@Component
@RequiredArgsConstructor
public class LessonCurrentUserProvider implements vn.edu.learningai.service.CurrentUserProvider {
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Long currentUserId() {
        AuthenticatedUser user = currentUserProvider.get();
        return user.userId();
    }
}
