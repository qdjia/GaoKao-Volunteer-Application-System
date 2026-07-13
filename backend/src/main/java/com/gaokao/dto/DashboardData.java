package com.gaokao.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardData {
    private Long totalStudents;
    private Long admittedStudents;
    private Long unadmittedStudents;
    private List<UniversityAdmissionStat> universityStats;
    private List<ScoreRangeStat> scoreRanges;

    @Data
    public static class UniversityAdmissionStat {
        private String universityName;
        private Long count;
    }

    @Data
    public static class ScoreRangeStat {
        private String range;
        private Long count;
    }
}