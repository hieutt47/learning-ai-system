package vn.edu.learningai.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.custom.CustomUserDetails;

@Component
@RequiredArgsConstructor
public class SecurityHelper {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
            || userDetails.getId() == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository
            .findById(userDetails.getId())
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }
        return user;
    }
}
