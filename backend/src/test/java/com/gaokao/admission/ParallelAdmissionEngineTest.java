package com.gaokao.admission;

import com.gaokao.domain.AdmissionResultStatus;
import com.gaokao.domain.AdmissionTraceAction;
import com.gaokao.domain.SubjectCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelAdmissionEngineTest {
    private final ParallelAdmissionEngine engine = new ParallelAdmissionEngine();

    @Test
    void separatesPhysicsAndHistoryQueues() {
        List<AdmissionDecision> decisions = engine.execute(
                List.of(
                        candidate(1, SubjectCategory.PHYSICS, 650, preference(11, 1, 1, true)),
                        candidate(2, SubjectCategory.HISTORY, 650, preference(21, 2, 1, true))),
                List.of(
                        plan(1, SubjectCategory.PHYSICS, 1),
                        plan(2, SubjectCategory.HISTORY, 1)),
                controlLines());

        assertThat(decisions).allMatch(decision -> decision.status() == AdmissionResultStatus.FILED);
    }

    @Test
    void filesAllCandidatesTiedAtSamePreferenceEvenPastCapacity() {
        AdmissionCandidateInput first = candidate(
                1, SubjectCategory.PHYSICS, 650, preference(11, 1, 1, true));
        AdmissionCandidateInput second = candidate(
                2, SubjectCategory.PHYSICS, 650, preference(12, 1, 1, true));

        List<AdmissionDecision> decisions = engine.execute(
                List.of(first, second),
                List.of(plan(1, SubjectCategory.PHYSICS, 1)),
                controlLines());

        assertThat(decisions).hasSize(2)
                .allMatch(decision -> decision.status() == AdmissionResultStatus.FILED);
    }

    @Test
    void givesPriorityToEarlierPreferenceWithinAnAcademicTie() {
        AdmissionCandidateInput earlierPreference = candidate(
                1, SubjectCategory.PHYSICS, 650, preference(11, 1, 1, true));
        AdmissionCandidateInput laterPreference = candidate(
                2, SubjectCategory.PHYSICS, 650, preference(12, 1, 2, true));

        Map<Long, AdmissionDecision> decisions = index(engine.execute(
                List.of(laterPreference, earlierPreference),
                List.of(plan(1, SubjectCategory.PHYSICS, 1)),
                controlLines()));

        assertThat(decisions.get(1L).status()).isEqualTo(AdmissionResultStatus.FILED);
        assertThat(decisions.get(2L).status()).isEqualTo(AdmissionResultStatus.SLIPPED);
    }

    @Test
    void producesAllFourStatusesAndExplainableTraces() {
        AdmissionCandidateInput highScore = candidate(
                1, SubjectCategory.PHYSICS, 700, preference(11, 1, 1, true));
        AdmissionCandidateInput slipped = candidate(
                2, SubjectCategory.PHYSICS, 690, preference(12, 1, 1, true));
        AdmissionCandidateInput belowLine = candidate(
                3, SubjectCategory.PHYSICS, 339, preference(13, 1, 1, true));
        AdmissionCandidateInput noValid = candidate(
                4, SubjectCategory.PHYSICS, 680, preference(14, 1, 1, false));

        Map<Long, AdmissionDecision> decisions = index(engine.execute(
                List.of(highScore, slipped, belowLine, noValid),
                List.of(plan(1, SubjectCategory.PHYSICS, 1)),
                controlLines()));

        assertThat(decisions.get(1L).status()).isEqualTo(AdmissionResultStatus.FILED);
        assertThat(decisions.get(2L).status()).isEqualTo(AdmissionResultStatus.SLIPPED);
        assertThat(decisions.get(3L).status()).isEqualTo(AdmissionResultStatus.BELOW_CONTROL_LINE);
        assertThat(decisions.get(4L).status()).isEqualTo(AdmissionResultStatus.NO_VALID_PREFERENCE);
        assertThat(decisions.get(2L).traces()).extracting(AdmissionTraceStep::action)
                .containsExactly(AdmissionTraceAction.QUOTA_FULL);
        assertThat(decisions.get(4L).traces()).extracting(AdmissionTraceStep::action)
                .containsExactly(AdmissionTraceAction.SKIPPED, AdmissionTraceAction.NO_VALID_PREFERENCE);
    }

    @Test
    void followsPreferencesAndStopsAfterFirstSuccessfulFiling() {
        AdmissionCandidateInput fillsFirstPlan = candidate(
                1, SubjectCategory.PHYSICS, 700, preference(11, 1, 1, true));
        AdmissionCandidateInput fallsBack = candidate(
                2,
                SubjectCategory.PHYSICS,
                680,
                preference(12, 1, 1, true),
                preference(13, 2, 2, true),
                preference(14, 3, 3, true));

        Map<Long, AdmissionDecision> decisions = index(engine.execute(
                List.of(fillsFirstPlan, fallsBack),
                List.of(
                        plan(1, SubjectCategory.PHYSICS, 1),
                        plan(2, SubjectCategory.PHYSICS, 1),
                        plan(3, SubjectCategory.PHYSICS, 1)),
                controlLines()));

        AdmissionDecision result = decisions.get(2L);
        assertThat(result.status()).isEqualTo(AdmissionResultStatus.FILED);
        assertThat(result.planSnapshotId()).isEqualTo(2L);
        assertThat(result.matchedPreferenceNo()).isEqualTo(2);
        assertThat(result.traces()).extracting(AdmissionTraceStep::preferenceNo)
                .containsExactly(1, 2);
    }

    @Test
    void returnsIdenticalDecisionsForIdenticalSnapshots() {
        List<AdmissionCandidateInput> candidates = List.of(
                candidate(2, SubjectCategory.PHYSICS, 650, preference(12, 1, 1, true)),
                candidate(1, SubjectCategory.PHYSICS, 650, preference(11, 1, 1, true)));
        List<AdmissionPlanInput> plans = List.of(plan(1, SubjectCategory.PHYSICS, 1));

        List<AdmissionDecision> first = engine.execute(candidates, plans, controlLines());
        List<AdmissionDecision> second = engine.execute(candidates, plans, controlLines());

        assertThat(second).isEqualTo(first);
    }

    private AdmissionCandidateInput candidate(
            long id,
            SubjectCategory category,
            int filingScore,
            AdmissionPreferenceInput... preferences
    ) {
        BigDecimal score = BigDecimal.valueOf(filingScore);
        AdmissionScore admissionScore = new AdmissionScore(
                score,
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(130),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(80));
        return new AdmissionCandidateInput(id, id, category, admissionScore, List.of(preferences));
    }

    private AdmissionPreferenceInput preference(
            long id,
            long planId,
            int preferenceNo,
            boolean eligible
    ) {
        return new AdmissionPreferenceInput(
                id, planId, preferenceNo, eligible, eligible ? null : "选科不匹配");
    }

    private AdmissionPlanInput plan(long id, SubjectCategory category, int capacity) {
        return new AdmissionPlanInput(id, category, capacity);
    }

    private Map<SubjectCategory, BigDecimal> controlLines() {
        Map<SubjectCategory, BigDecimal> lines = new EnumMap<>(SubjectCategory.class);
        lines.put(SubjectCategory.PHYSICS, BigDecimal.valueOf(340));
        lines.put(SubjectCategory.HISTORY, BigDecimal.valueOf(385));
        return lines;
    }

    private Map<Long, AdmissionDecision> index(List<AdmissionDecision> decisions) {
        Map<Long, AdmissionDecision> indexed = new java.util.HashMap<>();
        decisions.forEach(decision -> indexed.put(decision.sourceCandidateId(), decision));
        return indexed;
    }
}
