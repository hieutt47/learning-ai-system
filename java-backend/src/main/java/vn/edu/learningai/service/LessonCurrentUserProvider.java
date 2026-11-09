package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.learningai.security.AuthenticatedUser;
import vn.edu.learningai.security.CurrentUserProvider;

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
