package vn.edu.learningai.service.statistics;

import org.springframework.stereotype.Component;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

/**
 * Chính sách phân quyền theo đối tượng cho module thống kê:
 * - Thống kê khóa học / quiz: chỉ giảng viên sở hữu khóa học hoặc ADMIN.
 * - Thống kê hệ thống: chỉ ADMIN.
 */
@Component
public class StatisticsAccessPolicy {

    public void assertCanViewCourseStatistics(User actor, long courseOwnerId) {
        if (actor == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        boolean isCourseOwner = actor.getRole() == UserRole.TEACHER
            && actor.getId() != null
            && actor.getId().longValue() == courseOwnerId;
        if (!isCourseOwner) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    public void assertCanViewSystemStatistics(User actor) {
        if (actor == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        if (actor.getRole() != UserRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }
}
