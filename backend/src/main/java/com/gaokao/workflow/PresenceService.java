package com.gaokao.workflow;

import com.gaokao.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
    private final JdbcTemplate db;
    public PresenceService(JdbcTemplate db) { this.db=db; }
    public void heartbeat(AuthenticatedUser user) {
        if(!"STUDENT".equals(user.role())) return;
        db.update("UPDATE auth_session SET last_seen_at=clock_timestamp() WHERE session_id=? AND user_id=? AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP",
                user.sessionId(),user.userId());
    }
    public void offline(AuthenticatedUser user) {
        db.update("UPDATE auth_session SET last_seen_at=NULL WHERE session_id=? AND user_id=?",user.sessionId(),user.userId());
    }
    public int onlineCount() {
        return db.queryForObject("SELECT COUNT(DISTINCT s.user_id) FROM auth_session s JOIN sys_user u ON u.id=s.user_id " +
                "WHERE u.role='STUDENT' AND u.account_status='ACTIVE' AND s.revoked_at IS NULL AND s.expires_at>CURRENT_TIMESTAMP " +
                "AND s.last_seen_at>clock_timestamp()-INTERVAL '30 seconds'",Integer.class);
    }
}
