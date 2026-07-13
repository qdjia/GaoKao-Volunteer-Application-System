package com.gaokao.controller;

import com.gaokao.dto.LoginRequest;
import com.gaokao.dto.RegisterRequest;
import com.gaokao.entity.SysUser;
import com.gaokao.service.AuthService;
import com.gaokao.util.AuthInterceptor;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        SysUser user = authService.getUserInfo(request.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("username", user.getUsername());
        data.put("studentId", user.getStudentId());
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        SysUser user = authService.register(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("studentId", user.getStudentId());
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        authService.logout(token);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String info = AuthInterceptor.TOKEN_MAP.get(token);
        if (info == null) return Result.error(401, "未登录");
        String[] parts = info.split(":");
        Map<String, Object> data = new HashMap<>();
        data.put("username", parts[0]);
        data.put("role", parts[1]);
        if (parts.length > 2 && !"null".equals(parts[2])) {
            data.put("studentId", Long.parseLong(parts[2]));
        }
        return Result.success(data);
    }
}