package com.gaokao.admission;

import com.gaokao.domain.AdmissionResultStatus;
import com.gaokao.domain.SubjectCategory;

public record AdmissionResultView(
        long runId,
        int runNo,
        long candidateId,
        String examNumber,
        String candidateName,
        SubjectCategory category,
        AdmissionResultStatus status,
        String institutionCode,
        String institutionName,
        String groupCode,
        String groupName,
        Integer matchedPreferenceNo,
        String reason
) {
}
