package com.gaokao.admission;

import com.gaokao.domain.SubjectCategory;

import java.util.List;

public record AdmissionCandidateInput(
        long candidateSnapshotId,
        long sourceCandidateId,
        SubjectCategory category,
        AdmissionScore score,
        List<AdmissionPreferenceInput> preferences
) {
    public AdmissionCandidateInput {
        preferences = List.copyOf(preferences);
    }
}
