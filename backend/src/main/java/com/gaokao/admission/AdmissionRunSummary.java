package com.gaokao.admission;

public record AdmissionRunSummary(
        long runId,
        int runNo,
        int totalCandidates,
        int filedCandidates,
        int slippedCandidates,
        int belowControlLineCandidates,
        int noValidPreferenceCandidates
) {
}
