package com.gaokao.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RecommendResult {
    private Long universityId;
    private String universityName;
    private String type;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private String level;

    @Data
    public static class MajorRecommend {
        private Long majorId;
        private String majorName;
        private BigDecimal avgScore;
        private BigDecimal minScore;
        private String subjectReq;
        private boolean subjectMatch;
    }
}