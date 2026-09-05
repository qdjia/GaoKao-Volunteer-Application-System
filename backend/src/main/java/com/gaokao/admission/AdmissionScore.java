package com.gaokao.admission;

import java.math.BigDecimal;
import java.util.Comparator;

public record AdmissionScore(
        BigDecimal filingScore,
        BigDecimal chineseAndMathematics,
        BigDecimal chineseOrMathematicsMax,
        BigDecimal foreignLanguage,
        BigDecimal primarySubject,
        BigDecimal secondarySubjectMax,
        BigDecimal secondarySubjectMin
) {
    public static final Comparator<AdmissionScore> DESCENDING = Comparator
            .comparing(AdmissionScore::filingScore, Comparator.reverseOrder())
            .thenComparing(AdmissionScore::chineseAndMathematics, Comparator.reverseOrder())
            .thenComparing(AdmissionScore::chineseOrMathematicsMax, Comparator.reverseOrder())
            .thenComparing(AdmissionScore::foreignLanguage, Comparator.reverseOrder())
            .thenComparing(AdmissionScore::primarySubject, Comparator.reverseOrder())
            .thenComparing(AdmissionScore::secondarySubjectMax, Comparator.reverseOrder())
            .thenComparing(AdmissionScore::secondarySubjectMin, Comparator.reverseOrder());

    public static AdmissionScore of(
            BigDecimal cultureTotal,
            BigDecimal policyBonus,
            BigDecimal chinese,
            BigDecimal mathematics,
            BigDecimal foreignLanguage,
            BigDecimal primarySubject,
            BigDecimal secondarySubject1,
            BigDecimal secondarySubject2
    ) {
        return new AdmissionScore(
                cultureTotal.add(policyBonus),
                chinese.add(mathematics),
                chinese.max(mathematics),
                foreignLanguage,
                primarySubject,
                secondarySubject1.max(secondarySubject2),
                secondarySubject1.min(secondarySubject2)
        );
    }
}
