package com.gaokao.service;

import com.gaokao.config.SecurityProperties;
import com.gaokao.security.AccountLockedException;
import com.gaokao.security.AuthLoginResult;
import com.gaokao.security.AuthSessionStore;
import com.gaokao.security.AuthenticatedUser;
import com.gaokao.security.ClientNetworkPolicy;
import com.gaokao.security.JwtTokenService;
import com.gaokao.security.UnauthenticatedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final String DUMMY_HASH =
            "$2a$12$2b2f6f5S0Wq9IhVMcV7i8eCxEaYHxbRKmuh5kN5A3P6IY5fPFM.hK";

    private final AuthSessionStore store;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    public AuthService(
            AuthSessionStore store,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            SecurityProperties properties
    ) {
        this.store = store;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional(noRollbackFor = {UnauthenticatedException.class, AccountLockedException.class})
    public AuthLoginResult login(
            String username,
            String password,
            ClientNetworkPolicy.ClientContext client
    ) {
        String normalizedUsername = username == null ? "" : username.trim();
        AuthSessionStore.Account account = store.lockAccountByUsername(normalizedUsername);
        if (account == null) {
            passwordEncoder.matches(password == null ? "" : password, DUMMY_HASH);
            throw invalidCredentials();
        }
        if (!"ACTIVE".equals(account.status())) {
            store.revokeActiveSessions(account.id(), "ACCOUNT_DISABLED");
            throw new UnauthenticatedException("账号已禁用");
        }

        Instant now = Instant.now();
        if (account.lockedUntil() != null && account.lockedUntil().isAfter(now)) {
            throw new AccountLockedException("登录失败次数过多，请15分钟后重试");
        }
        if (!passwordEncoder.matches(password == null ? "" : password, account.passwordHash())) {
            int attempts = account.failedLoginAttempts() + 1;
            Instant lockedUntil = attempts >= properties.getMaxLoginAttempts()
                    ? now.plus(properties.getLockDuration()) : null;
            store.updateFailedLogin(account.id(), attempts, lockedUntil);
            if (lockedUntil != null) {
                throw new AccountLockedException("登录失败次数过多，请15分钟后重试");
            }
            throw invalidCredentials();
        }

        if (!"ADMIN".equals(account.role()) && !"STUDENT".equals(account.role())) {
            throw new UnauthenticatedException("当前系统仅支持管理员和考生登录");
        }
        if ("ADMIN".equals(account.role()) && !client.local()) {
            throw new SecurityException("管理员仅允许从服务器本机登录");
        }

        store.clearFailedLogin(account.id());
        store.revokeActiveSessions(account.id(), "NEW_LOGIN");
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = now.plus(properties.getSessionDuration());
        String token = jwtTokenService.issue(
                sessionId, account.username(), account.role(), now, expiresAt);
        String audience = "ADMIN".equals(account.role()) ? "LOCAL_ADMIN" : "PUBLIC_CANDIDATE";
        store.createSession(
                sessionId, account.id(), jwtTokenService.hash(token), audience, now, expiresAt,
                client.ipHash(), client.userAgentHash());
        return new AuthLoginResult(
                token, account.role(), account.username(), account.studentId(),
                account.mustChangePassword(), expiresAt);
    }

    @Transactional
    public void logout(AuthenticatedUser user) {
        store.revokeSession(user.sessionId(), "LOGOUT");
    }

    @Transactional
    public void changePassword(AuthenticatedUser user, String currentPassword, String newPassword) {
        validateNewPassword(newPassword);
        AuthSessionStore.Account account = store.lockAccountById(user.userId());
        if (account == null || !"ACTIVE".equals(account.status())) {
            throw new UnauthenticatedException("账号不存在或已禁用");
        }
        if (!passwordEncoder.matches(currentPassword == null ? "" : currentPassword, account.passwordHash())) {
            throw new UnauthenticatedException("当前密码错误");
        }
        if (passwordEncoder.matches(newPassword, account.passwordHash())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        store.updatePassword(account.id(), passwordEncoder.encode(newPassword));
        store.revokeActiveSessions(account.id(), "PASSWORD_CHANGED");
    }

    @Transactional
    public void setAccountStatus(long operatorUserId, long targetUserId, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("账号状态只能为ACTIVE或DISABLED");
        }
        if (operatorUserId == targetUserId && "DISABLED".equals(status)) {
            throw new IllegalArgumentException("管理员不能禁用当前登录账号");
        }
        store.updateAccountStatus(targetUserId, status);
        if ("DISABLED".equals(status)) {
            store.revokeActiveSessions(targetUserId, "ACCOUNT_DISABLED");
        }
    }

    private void validateNewPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("新密码须为8至72位，并同时包含字母和数字");
        }
    }

    private UnauthenticatedException invalidCredentials() {
        return new UnauthenticatedException("用户名或密码错误");
    }
}
