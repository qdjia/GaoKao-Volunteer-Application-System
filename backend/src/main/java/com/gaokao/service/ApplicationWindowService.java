package com.gaokao.service;

import com.gaokao.config.ApplicationWindowProperties;
import com.gaokao.dto.ApplicationWindowStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class ApplicationWindowService {

    private final ApplicationWindowProperties properties;
    private final Clock clock;

    @Autowired
    public ApplicationWindowService(ApplicationWindowProperties properties) {
        this(properties, Clock.system(ZoneId.of(properties.getZoneId())));
    }

    ApplicationWindowService(ApplicationWindowProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ApplicationWindowStatus getStatus() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = properties.getStart();
        LocalDateTime end = properties.getEnd();
        boolean configured = start != null && end != null && !end.isBefore(start);
        boolean open = configured && !now.isBefore(start) && !now.isAfter(end);

        String message;
        if (!configured) {
            message = "志愿填报时间配置无效，请联系管理员";
        } else if (now.isBefore(start)) {
            message = "志愿填报尚未开始";
        } else if (now.isAfter(end)) {
            message = "志愿填报已截止";
        } else {
            message = "志愿填报进行中";
        }
        return new ApplicationWindowStatus(start, end, now, open, message);
    }

    public void requireOpen() {
        ApplicationWindowStatus status = getStatus();
        if (!status.open()) {
            throw new RuntimeException(status.message());
        }
    }
}
