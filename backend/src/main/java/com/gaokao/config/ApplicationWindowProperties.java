package com.gaokao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@Component
@ConfigurationProperties(prefix = "gaokao.application-window")
public class ApplicationWindowProperties {
    private LocalDateTime start;
    private LocalDateTime end;
    private String zoneId = "Asia/Shanghai";
}
