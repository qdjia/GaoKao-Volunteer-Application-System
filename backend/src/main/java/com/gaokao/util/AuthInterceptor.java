package com.gaokao.util;

import com.gaokao.security.AuthTokenService;
import com.gaokao.security.AuthenticatedUser;
import com.gaokao.security.ClientNetworkPolicy;
import com.gaokao.security.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthTokenService authTokenService;
    private final ClientNetworkPolicy networkPolicy;

    public AuthInterceptor(AuthTokenService authTokenService, ClientNetworkPolicy networkPolicy) {
        this.authTokenService = authTokenService;
        this.networkPolicy = networkPolicy;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = bearerToken(request);
        if (token == null) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }
        try {
            AuthenticatedUser user = authTokenService.authenticate(token);
            ClientNetworkPolicy.ClientContext client = networkPolicy.describe(request);
            if ("ADMIN".equals(user.role()) && !client.local()) {
                writeError(response, 403, "管理功能仅允许从服务器本机访问");
                return false;
            }
            if (user.mustChangePassword() && !isPasswordChangePath(request.getRequestURI())) {
                writeError(response, 428, "首次登录必须修改初始密码");
                return false;
            }
            request.setAttribute("authenticatedUser", user);
            request.setAttribute("currentUser", user);
            request.setAttribute("localRequest", client.local());
            return true;
        } catch (UnauthenticatedException | SecurityException e) {
            writeError(response, 401, e.getMessage());
            return false;
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private boolean isPasswordChangePath(String uri) {
        return uri.endsWith("/api/auth/change-password")
                || uri.endsWith("/api/auth/logout")
                || uri.endsWith("/api/auth/info");
    }

    private void writeError(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":" + status + ",\"message\":\"" + jsonEscape(message) + "\"}");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("无法写入认证响应", e);
        }
    }

    private String jsonEscape(String value) {
        return value == null ? "认证失败" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
