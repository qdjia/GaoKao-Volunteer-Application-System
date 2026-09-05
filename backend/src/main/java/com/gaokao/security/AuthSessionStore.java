package com.gaokao.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class AuthSessionStore {
    private final JdbcTemplate jdbcTemplate;

    public AuthSessionStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Account lockAccountByUsername(String username) {
        List<Account> rows = jdbcTemplate.query(
                "SELECT id, username, password, role, student_id, account_status, " +
                        "failed_login_attempts, locked_until, must_change_password " +
                        "FROM sys_user WHERE username = ? FOR UPDATE",
                (rs, rowNum) -> new Account(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        nullableLong(rs.getObject("student_id")),
                        rs.getString("account_status"),
                        rs.getInt("failed_login_attempts"),
                        toInstant(rs.getTimestamp("locked_until")),
                        rs.getBoolean("must_change_password")),
                username);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Account lockAccountById(long userId) {
        List<Account> rows = jdbcTemplate.query(
                "SELECT id, username, password, role, student_id, account_status, " +
                        "failed_login_attempts, locked_until, must_change_password " +
                        "FROM sys_user WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> new Account(
                        rs.getLong("id"), rs.getString("username"), rs.getString("password"),
                        rs.getString("role"), nullableLong(rs.getObject("student_id")),
                        rs.getString("account_status"), rs.getInt("failed_login_attempts"),
                        toInstant(rs.getTimestamp("locked_until")), rs.getBoolean("must_change_password")),
                userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateFailedLogin(long userId, int attempts, Instant lockedUntil) {
        jdbcTemplate.update(
                "UPDATE sys_user SET failed_login_attempts = ?, locked_until = ?, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                attempts, timestamp(lockedUntil), userId);
    }

    public void clearFailedLogin(long userId) {
        jdbcTemplate.update(
                "UPDATE sys_user SET failed_login_attempts = 0, locked_until = NULL, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                userId);
    }

    public void updatePassword(long userId, String passwordHash) {
        jdbcTemplate.update(
                "UPDATE sys_user SET password = ?, must_change_password = FALSE, " +
                        "password_changed_at = CURRENT_TIMESTAMP, failed_login_attempts = 0, " +
                        "locked_until = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                passwordHash, userId);
    }

    public void updateAccountStatus(long userId, String status) {
        int updated = jdbcTemplate.update(
                "UPDATE sys_user SET account_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status, userId);
        if (updated != 1) {
            throw new IllegalArgumentException("账号不存在");
        }
    }

    public void revokeActiveSessions(long userId, String reason) {
        jdbcTemplate.update(
                "UPDATE auth_session SET revoked_at = CURRENT_TIMESTAMP, revoke_reason = ? " +
                        "WHERE user_id = ? AND revoked_at IS NULL",
                reason, userId);
    }

    public void revokeSession(UUID sessionId, String reason) {
        jdbcTemplate.update(
                "UPDATE auth_session SET revoked_at = CURRENT_TIMESTAMP, revoke_reason = ? " +
                        "WHERE session_id = ? AND revoked_at IS NULL",
                reason, sessionId);
    }

    public void createSession(
            UUID sessionId,
            long userId,
            String tokenHash,
            String audience,
            Instant createdAt,
            Instant expiresAt,
            String clientIpHash,
            String userAgentHash
    ) {
        jdbcTemplate.update(
                "INSERT INTO auth_session(session_id, user_id, token_hash, audience, created_at, " +
                        "expires_at, client_ip_hash, user_agent_hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                sessionId, userId, tokenHash, audience, timestamp(createdAt), timestamp(expiresAt),
                clientIpHash, userAgentHash);
    }

    public SessionAccount findActiveSession(UUID sessionId, String tokenHash) {
        List<SessionAccount> rows = jdbcTemplate.query(
                "SELECT s.session_id, s.audience, s.expires_at, u.id AS user_id, u.username, u.role, " +
                        "u.student_id, u.account_status, u.must_change_password " +
                        "FROM auth_session s JOIN sys_user u ON u.id = s.user_id " +
                        "WHERE s.session_id = ? AND s.token_hash = ? AND s.revoked_at IS NULL " +
                        "AND s.expires_at > CURRENT_TIMESTAMP",
                (rs, rowNum) -> new SessionAccount(
                        rs.getObject("session_id", UUID.class), rs.getString("audience"),
                        toInstant(rs.getTimestamp("expires_at")), rs.getLong("user_id"),
                        rs.getString("username"), rs.getString("role"),
                        nullableLong(rs.getObject("student_id")), rs.getString("account_status"),
                        rs.getBoolean("must_change_password")),
                sessionId, tokenHash);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<LegacyPassword> findPasswordsNeedingMigration() {
        return jdbcTemplate.query(
                "SELECT id, password FROM sys_user WHERE password NOT LIKE '$2a$%' " +
                        "AND password NOT LIKE '$2b$%' AND password NOT LIKE '$2y$%'",
                (rs, rowNum) -> new LegacyPassword(rs.getLong("id"), rs.getString("password")));
    }

    public void replaceLegacyPassword(long userId, String passwordHash) {
        jdbcTemplate.update(
                "UPDATE sys_user SET password = ?, password_changed_at = CURRENT_TIMESTAMP, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                passwordHash, userId);
    }

    private Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    public record Account(
            long id, String username, String passwordHash, String role, Long studentId,
            String status, int failedLoginAttempts, Instant lockedUntil, boolean mustChangePassword
    ) {
    }

    public record SessionAccount(
            UUID sessionId, String audience, Instant expiresAt, long userId, String username,
            String role, Long studentId, String status, boolean mustChangePassword
    ) {
    }

    public record LegacyPassword(long userId, String value) {
    }
}
