package com.gaokao.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {
    private final JwtTokenService jwtTokenService;
    private final AuthSessionStore store;

    public AuthTokenService(JwtTokenService jwtTokenService, AuthSessionStore store) {
        this.jwtTokenService = jwtTokenService;
        this.store = store;
    }

    @Transactional(noRollbackFor = UnauthenticatedException.class)
    public AuthenticatedUser authenticate(String token) {
        JwtTokenService.ParsedToken parsed = jwtTokenService.parseAndVerify(token);
        AuthSessionStore.SessionAccount session = store.findActiveSession(
                parsed.sessionId(), jwtTokenService.hash(token));
        if (session == null
                || !session.username().equals(parsed.username())
                || !session.role().equals(parsed.role())
                || session.expiresAt().getEpochSecond() != parsed.expiresAt().getEpochSecond()) {
            throw new UnauthenticatedException("未登录或登录已过期");
        }
        if (!"ACTIVE".equals(session.status())) {
            store.revokeActiveSessions(session.userId(), "ACCOUNT_DISABLED");
            throw new UnauthenticatedException("账号已禁用");
        }
        String expectedAudience = "ADMIN".equals(session.role())
                ? "LOCAL_ADMIN" : "PUBLIC_CANDIDATE";
        if (!expectedAudience.equals(session.audience())) {
            store.revokeSession(session.sessionId(), "INVALID_AUDIENCE");
            throw new UnauthenticatedException("登录会话无效");
        }
        return new AuthenticatedUser(
                session.userId(), session.username(), session.role(), session.studentId(),
                session.sessionId(), session.audience(), session.mustChangePassword());
    }
}
