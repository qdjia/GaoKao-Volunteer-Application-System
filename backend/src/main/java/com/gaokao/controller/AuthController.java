package com.gaokao.controller;

import com.gaokao.dto.ChangePasswordRequest;
import com.gaokao.dto.LoginRequest;
import com.gaokao.security.AuthLoginResult;
import com.gaokao.security.ClientNetworkPolicy;
import com.gaokao.service.AuthService;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final ClientNetworkPolicy networkPolicy;

    public AuthController(AuthService authService, ClientNetworkPolicy networkPolicy) {
        this.authService = authService;
        this.networkPolicy = networkPolicy;
    }

    @PostMapping("/login")
    public Result<AuthLoginResult> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(authService.login(
                request.getUsername(), request.getPassword(), networkPolicy.describe(httpRequest)));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(AuthContext.authenticatedUser(request));
        return Result.success();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest changeRequest,
            HttpServletRequest request
    ) {
        authService.changePassword(
                AuthContext.authenticatedUser(request),
                changeRequest.currentPassword(),
                changeRequest.newPassword());
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(HttpServletRequest request) {
        AuthContext.CurrentUser user = AuthContext.currentUser(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", user.username());
        data.put("role", user.role());
        data.put("studentId", user.studentId());
        data.put("mustChangePassword", user.mustChangePassword());
        return Result.success(data);
    }
}
