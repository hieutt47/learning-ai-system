package vn.edu.learningai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.security.custom.CustomUserDetails;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {
    @Override
    public AuthenticatedUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        if (!principal.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }
        return new AuthenticatedUser(principal.getId(), principal.getRole());
    }
}
