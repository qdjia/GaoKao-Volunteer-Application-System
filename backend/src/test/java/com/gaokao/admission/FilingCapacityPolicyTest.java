package com.gaokao.admission;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilingCapacityPolicyTest {
    private final FilingCapacityPolicy policy = new FilingCapacityPolicy();

    @Test
    void usesOneToOneCapacityAtOneHundredPercent() {
        assertThat(policy.calculate(20, new BigDecimal("1.0000"))).isEqualTo(20);
    }

    @Test
    void floorsCapacitySoConfiguredRatioIsNeverExceeded() {
        assertThat(policy.calculate(20, new BigDecimal("1.0500"))).isEqualTo(21);
        assertThat(policy.calculate(10, new BigDecimal("1.0500"))).isEqualTo(10);
    }

    @Test
    void rejectsRatiosOutsideOfficialRange() {
        assertThatThrownBy(() -> policy.calculate(10, new BigDecimal("0.9999")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.calculate(10, new BigDecimal("1.0501")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
