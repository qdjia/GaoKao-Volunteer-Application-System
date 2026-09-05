package com.gaokao.admission;

public record AdmissionPreferenceInput(
        long preferenceSnapshotId,
        long planSnapshotId,
        int preferenceNo,
        boolean eligible,
        String invalidReason
) {
}
