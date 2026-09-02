package com.gaokao.dto;

import java.time.LocalDateTime;

public record ApplicationWindowStatus(
        LocalDateTime start,
        LocalDateTime end,
        LocalDateTime current,
        boolean open,
        String message
) {
}
