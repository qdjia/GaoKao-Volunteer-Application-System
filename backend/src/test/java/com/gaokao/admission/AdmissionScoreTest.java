package com.gaokao.admission;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AdmissionScoreTest {

    @Test
    void comparesEveryOfficialAcademicTieBreakInOrder() {
        AdmissionScore baseline = score(600, 250, 130, 120, 90, 85, 80);

        assertHigher(score(601, 1, 1, 1, 1, 1, 1), baseline);
        assertHigher(score(600, 251, 1, 1, 1, 1, 1), baseline);
        assertHigher(score(600, 250, 131, 1, 1, 1, 1), baseline);
        assertHigher(score(600, 250, 130, 121, 1, 1, 1), baseline);
        assertHigher(score(600, 250, 130, 120, 91, 1, 1), baseline);
        assertHigher(score(600, 250, 130, 120, 90, 86, 1), baseline);
        assertHigher(score(600, 250, 130, 120, 90, 85, 81), baseline);
    }

    private void assertHigher(AdmissionScore higher, AdmissionScore lower) {
        assertThat(AdmissionScore.DESCENDING.compare(higher, lower)).isLessThan(0);
    }

    private AdmissionScore score(
            int filing,
            int chineseAndMath,
            int chineseOrMathMax,
            int foreign,
            int primary,
            int secondaryMax,
            int secondaryMin
    ) {
        return new AdmissionScore(
                decimal(filing),
                decimal(chineseAndMath),
                decimal(chineseOrMathMax),
                decimal(foreign),
                decimal(primary),
                decimal(secondaryMax),
                decimal(secondaryMin));
    }

    private BigDecimal decimal(int value) {
        return BigDecimal.valueOf(value);
    }
}
