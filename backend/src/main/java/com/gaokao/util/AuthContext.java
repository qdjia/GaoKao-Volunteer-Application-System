package com.gaokao.util;

import jakarta.servlet.http.HttpServletRequest;

public final class AuthContext {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_TEACHER = "TEACHER";

    private AuthContext() {
    }

    public static CurrentUser currentUser(HttpServletRequest request) {
        Object raw = request.getAttribute("currentUser");
        if (raw == null) {
            throw new SecurityException("未登录或登录已过期");
        }

        String[] parts = raw.toString().split(":", -1);
        if (parts.length < 2) {
            throw new SecurityException("登录信息异常");
        }

        Long studentId = null;
        if (parts.length > 2 && parts[2] != null && !parts[2].isBlank() && !"null".equals(parts[2])) {
            studentId = Long.parseLong(parts[2]);
        }
        return new CurrentUser(parts[0], parts[1], studentId);
    }

    public static CurrentUser requireAdmin(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (!user.isAdmin()) {
            throw new SecurityException("无权限访问");
        }
        return user;
    }

    public static CurrentUser requireAdminOrSelf(HttpServletRequest request, Long studentId) {
        CurrentUser user = currentUser(request);
        if (user.isAdmin()) {
            return user;
        }
        if (user.isStudent() && studentId != null && studentId.equals(user.studentId())) {
            return user;
        }
        throw new SecurityException("无权限访问他人数据");
    }

    public record CurrentUser(String username, String role, Long studentId) {
        public boolean isAdmin() {
            return ROLE_ADMIN.equals(role);
        }

        public boolean isStudent() {
            return ROLE_STUDENT.equals(role);
        }

        public boolean isTeacher() {
            return ROLE_TEACHER.equals(role);
        }
    }
}
