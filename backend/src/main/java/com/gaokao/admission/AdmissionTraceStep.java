package com.gaokao.admission;

import com.gaokao.domain.AdmissionTraceAction;

public record AdmissionTraceStep(
        int sequenceNo,
        Long planSnapshotId,
        Integer preferenceNo,
        AdmissionTraceAction action,
        String detail
) {
}
