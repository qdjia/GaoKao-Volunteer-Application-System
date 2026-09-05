package com.gaokao.admission;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FilingCapacityPolicy {

    public int calculate(int plannedCount, BigDecimal filingRatio) {
        if (plannedCount <= 0) {
            throw new IllegalArgumentException("招生计划数必须大于0");
        }
        if (filingRatio == null
                || filingRatio.compareTo(BigDecimal.ONE) < 0
                || filingRatio.compareTo(new BigDecimal("1.05")) > 0) {
            throw new IllegalArgumentException("投档比例必须在100%到105%之间");
        }
        return filingRatio.multiply(BigDecimal.valueOf(plannedCount))
                .setScale(0, RoundingMode.FLOOR)
                .intValueExact();
    }
}
