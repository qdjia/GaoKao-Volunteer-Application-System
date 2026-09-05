package com.gaokao.admission;

import com.gaokao.domain.AdmissionResultStatus;

import java.util.List;

public record AdmissionDecision(
        long candidateSnapshotId,
        long sourceCandidateId,
        AdmissionResultStatus status,
        Long planSnapshotId,
        Integer matchedPreferenceNo,
        String reason,
        List<AdmissionTraceStep> traces
) {
    public AdmissionDecision {
        traces = List.copyOf(traces);
    }
}
