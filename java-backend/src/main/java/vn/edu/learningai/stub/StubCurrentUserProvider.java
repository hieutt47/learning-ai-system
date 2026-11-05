package vn.edu.learningai.stub;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.security.CurrentUserProvider;

import java.util.List;

/**
 * TODO(stub): thay bằng user từ JWT khi nhánh auth merge. Tạm đọc header X-User-Id (chỉ dùng khi dev,
 * không an toàn cho production) và kiểm tra user tồn tại + đang active.
 */
@Component
@RequiredArgsConstructor
public class StubCurrentUserProvider implements CurrentUserProvider {

    public static final String HEADER = "X-User-Id";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Long currentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String header = attributes == null ? null : attributes.getRequest().getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        long userId;
        try {
            userId = Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        List<Boolean> active = jdbcTemplate.queryForList(
            "SELECT is_active FROM users WHERE id = ?", Boolean.class, userId);
        if (active.isEmpty()) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        if (!Boolean.TRUE.equals(active.get(0))) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Account is inactive");
        }
        return userId;
    }
}
