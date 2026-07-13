package com.gaokao.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Application {
    private Long id;
    private Long studentId;
    private Long universityId;
    private Integer priority;
    private Boolean acceptAdjust;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String universityName;
    private String studentName;
    private List<ApplicationMajor> majors;
}