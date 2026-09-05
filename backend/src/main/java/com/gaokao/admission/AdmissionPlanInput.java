package com.gaokao.admission;

import com.gaokao.domain.SubjectCategory;

public record AdmissionPlanInput(
        long planSnapshotId,
        SubjectCategory category,
        int capacity
) {
}
