package com.gaokao.service;

import com.gaokao.config.ApplicationWindowProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationWindowServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void isOpenInsideConfiguredWindow() {
        ApplicationWindowService service = serviceAt("2026-06-25T10:00:00");

        assertTrue(service.getStatus().open());
        assertDoesNotThrow(service::requireOpen);
    }

    @Test
    void rejectsWritesBeforeWindowStarts() {
        ApplicationWindowService service = serviceAt("2026-06-19T23:59:59");

        assertFalse(service.getStatus().open());
        RuntimeException error = assertThrows(RuntimeException.class, service::requireOpen);
        assertEquals("志愿填报尚未开始", error.getMessage());
    }

    @Test
    void rejectsWritesAfterWindowEnds() {
        ApplicationWindowService service = serviceAt("2026-07-06T00:00:00");

        RuntimeException error = assertThrows(RuntimeException.class, service::requireOpen);
        assertEquals("志愿填报已截止", error.getMessage());
    }

    private ApplicationWindowService serviceAt(String localDateTime) {
        ApplicationWindowProperties properties = new ApplicationWindowProperties();
        properties.setStart(LocalDateTime.parse("2026-06-20T00:00:00"));
        properties.setEnd(LocalDateTime.parse("2026-07-05T23:59:59"));
        properties.setZoneId(ZONE.getId());
        Instant instant = LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant();
        return new ApplicationWindowService(properties, Clock.fixed(instant, ZONE));
    }
}
