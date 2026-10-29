package vn.edu.learningai.security;

import lombok.RequiredArgsConstructor;
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
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository
            .findById(userDetails.getId())
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return user;
    }
}
