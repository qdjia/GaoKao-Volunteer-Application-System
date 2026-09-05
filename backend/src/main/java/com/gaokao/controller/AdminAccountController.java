package com.gaokao.controller;

import com.gaokao.dto.AccountStatusRequest;
import com.gaokao.service.AuthService;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {
    private final AuthService authService;

    public AdminAccountController(AuthService authService) {
        this.authService = authService;
    }

    @PatchMapping("/{userId}/status")
    public Result<Void> setStatus(
            @PathVariable long userId,
            @Valid @RequestBody AccountStatusRequest statusRequest,
            HttpServletRequest request
    ) {
        AuthContext.CurrentUser admin = AuthContext.requireAdmin(request);
        authService.setAccountStatus(admin.userId(), userId, statusRequest.status());
        return Result.success();
    }
}
