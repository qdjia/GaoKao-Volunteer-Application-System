package com.gaokao.admission;

import com.gaokao.domain.AdmissionTraceAction;

public record AdmissionTraceView(
        int sequenceNo,
        Integer preferenceNo,
        String institutionCode,
        String institutionName,
        String groupCode,
        String groupName,
        AdmissionTraceAction action,
        String detail
) {
}
