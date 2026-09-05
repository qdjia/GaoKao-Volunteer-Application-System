package com.gaokao.util;

import com.gaokao.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthContext {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";

    private AuthContext() {
    }

    public static AuthenticatedUser authenticatedUser(HttpServletRequest request) {
        Object raw = request.getAttribute("authenticatedUser");
        if (!(raw instanceof AuthenticatedUser user)) {
            throw new SecurityException("未登录或登录已过期");
        }
        return user;
    }

    public static CurrentUser currentUser(HttpServletRequest request) {
        AuthenticatedUser user = authenticatedUser(request);
        return new CurrentUser(
                user.userId(), user.username(), user.role(), user.studentId(), user.mustChangePassword());
    }

    public static CurrentUser requireAdmin(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (!user.isAdmin()) {
            throw new SecurityException("无权限访问");
        }
        if (!Boolean.TRUE.equals(request.getAttribute("localRequest"))) {
            throw new SecurityException("管理功能仅允许从服务器本机访问");
        }
        return user;
    }

    public static CurrentUser requireAdminOrSelf(HttpServletRequest request, Long studentId) {
        CurrentUser user = currentUser(request);
        if (user.isAdmin()) {
            if (!Boolean.TRUE.equals(request.getAttribute("localRequest"))) {
                throw new SecurityException("管理功能仅允许从服务器本机访问");
            }
            return user;
        }
        if (user.isStudent() && studentId != null && studentId.equals(user.studentId())) {
            return user;
        }
        throw new SecurityException("无权限访问他人数据");
    }

    public record CurrentUser(
            long userId,
            String username,
            String role,
            Long studentId,
            boolean mustChangePassword
    ) {
        public boolean isAdmin() {
            return ROLE_ADMIN.equals(role);
        }

        public boolean isStudent() {
            return ROLE_STUDENT.equals(role);
        }
    }
}
