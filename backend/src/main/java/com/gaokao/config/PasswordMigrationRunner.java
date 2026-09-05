package com.gaokao.config;

import com.gaokao.security.AuthSessionStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(100)
public class PasswordMigrationRunner implements CommandLineRunner {
    private final AuthSessionStore store;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(AuthSessionStore store, PasswordEncoder passwordEncoder) {
        this.store = store;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (AuthSessionStore.LegacyPassword password : store.findPasswordsNeedingMigration()) {
            store.replaceLegacyPassword(password.userId(), passwordEncoder.encode(password.value()));
        }
    }
}
