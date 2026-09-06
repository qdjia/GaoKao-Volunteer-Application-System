package com.gaokao.workflow;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gaokao.workflow")
public class WorkflowProperties {
    public enum Mode { DEMO, PRODUCTION }
    private Mode mode = Mode.PRODUCTION;
    private String noticeVersion = "2026-1";
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public String getNoticeVersion() { return noticeVersion; }
    public void setNoticeVersion(String noticeVersion) { this.noticeVersion = noticeVersion; }
    public boolean isDemo() { return mode == Mode.DEMO; }
}
