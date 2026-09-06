package com.gaokao.excel;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

@Component
public class PermanentDemoAccounts {
    public static final int COUNT = 10;
    private final JdbcTemplate db;

    public PermanentDemoAccounts(JdbcTemplate db) { this.db = db; }

    public static String username(int slot) {
        if (slot < 1 || slot > COUNT) throw new IllegalArgumentException("体验账号序号无效");
        return String.format(Locale.ROOT, "9%09d", slot);
    }

    public static int slot(String username) {
        for (int slot = 1; slot <= COUNT; slot++) if (username(slot).equals(username)) return slot;
        return 0;
    }

    public Map<String, String> identities() {
        List<byte[]> stored = db.query("SELECT secret FROM demo_account_secret WHERE id=1", (rs, row) -> rs.getBytes(1));
        if (stored.isEmpty()) {
            if (db.queryForObject("SELECT COUNT(*) FROM sys_user WHERE demo_slot IS NOT NULL", Integer.class) > 0)
                throw new IllegalStateException("固定体验凭据缺失，请恢复数据库备份，不能重新生成密码");
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            db.update("INSERT INTO demo_account_secret(id,secret) VALUES (1,?) ON CONFLICT(id) DO NOTHING", generated);
            stored = db.query("SELECT secret FROM demo_account_secret WHERE id=1", (rs, row) -> rs.getBytes(1));
        }
        // Only fictitious demo credentials are recoverable; real candidate passwords remain BCrypt-only.
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stored.get(0), "HmacSHA256"));
            Map<String, String> result = new LinkedHashMap<>();
            for (int slot = 1; slot <= COUNT; slot++) {
                long number = Integer.toUnsignedLong(ByteBuffer.wrap(mac.doFinal(
                        ("gaokao-fixed-demo-v1:" + slot).getBytes(StandardCharsets.US_ASCII))).getInt());
                String password = String.format(Locale.ROOT, "%06d", number % 1000000);
                result.put(username(slot), "999999200801" + password);
            }
            return result;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("无法读取固定体验凭据");
        }
    }
}
