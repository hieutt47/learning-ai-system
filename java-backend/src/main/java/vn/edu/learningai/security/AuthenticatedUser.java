package vn.edu.learningai.security;

import vn.edu.learningai.common.enums.UserRole;

public record AuthenticatedUser(Long userId, UserRole role) {
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isTeacherOrAdmin() {
        return role == UserRole.TEACHER || role == UserRole.ADMIN;
    }
}
