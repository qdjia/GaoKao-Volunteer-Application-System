package com.gaokao.dto;

import lombok.Data;
import java.util.List;

@Data
public class ApplicationSubmitRequest {
    private Long studentId;
    private String status;
    private List<ApplicationItem> applications;

    @Data
    public static class ApplicationItem {
        private Long universityId;
        private Integer priority;
        private Boolean acceptAdjust;
        private List<MajorItem> majors;
    }

    @Data
    public static class MajorItem {
        private Long majorId;
        private Integer priority;
    }
}