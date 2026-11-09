package vn.edu.learningai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.security.custom.CustomUserDetails;

/**
 * Identity taken from the authenticated request, populated by
 * {@link vn.edu.learningai.security.jwt.JwtAuthenticationFilter}.
 *
 * <p>Replaces the temporary {@code X-User-Id} provider now that the auth module
 * exists. Services still depend only on {@link CurrentUserProvider}, so this
 * swap did not touch them.</p>
 *
 * <p>Routes are already guarded by Security's URL rules, but this check is not
 * redundant: the service layer is the only place that knows which roles may act
 * on which object, and a controller reached from a second entry point (a test,
 * an internal caller) must still fail closed.</p>
 */
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
            // Disabled after the token was issued: treat as unauthenticated.
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }
        return new AuthenticatedUser(principal.getId(), principal.getRole());
    }
}
