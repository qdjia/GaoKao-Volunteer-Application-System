package com.gaokao.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public final class DatabaseTime {
    private DatabaseTime() {}

    // Domain TIMESTAMP columns contain UTC wall time, independent of the JVM zone.
    public static Instant instant(Object value) {
        return value == null ? null : ((Timestamp) value).toLocalDateTime().toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime utc(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    public static List<Map<String, Object>> normalize(List<Map<String, Object>> rows, String... columns) {
        for (var row : rows) {
            for (String column : columns) row.put(column, instant(row.get(column)));
        }
        return rows;
    }
}
