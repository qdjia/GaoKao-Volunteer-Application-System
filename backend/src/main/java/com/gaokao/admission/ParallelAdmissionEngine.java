package com.gaokao.admission;

import com.gaokao.domain.AdmissionResultStatus;
import com.gaokao.domain.AdmissionTraceAction;
import com.gaokao.domain.SubjectCategory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ParallelAdmissionEngine {

    public List<AdmissionDecision> execute(
            List<AdmissionCandidateInput> candidates,
            List<AdmissionPlanInput> plans,
            Map<SubjectCategory, BigDecimal> controlLines
    ) {
        Map<Long, AdmissionPlanInput> plansById = indexPlans(plans);
        Map<Long, Integer> filedCounts = new HashMap<>();
        List<AdmissionDecision> decisions = new ArrayList<>();

        for (SubjectCategory category : SubjectCategory.values()) {
            BigDecimal controlLine = controlLines.get(category);
            if (controlLine == null) {
                throw new IllegalArgumentException("缺少" + category + "录取控制线");
            }

            List<AdmissionCandidateInput> queue = candidates.stream()
                    .filter(candidate -> candidate.category() == category)
                    .sorted(Comparator.comparing(AdmissionCandidateInput::score, AdmissionScore.DESCENDING))
                    .toList();

            int offset = 0;
            while (offset < queue.size()) {
                int end = offset + 1;
                while (end < queue.size()
                        && AdmissionScore.DESCENDING.compare(queue.get(offset).score(), queue.get(end).score()) == 0) {
                    end++;
                }
                processAcademicTieGroup(
                        queue.subList(offset, end), controlLine, plansById, filedCounts, decisions);
                offset = end;
            }
        }

        return decisions.stream()
                .sorted(Comparator.comparingLong(AdmissionDecision::sourceCandidateId))
                .toList();
    }

    private void processAcademicTieGroup(
            List<AdmissionCandidateInput> tieGroup,
            BigDecimal controlLine,
            Map<Long, AdmissionPlanInput> plansById,
            Map<Long, Integer> filedCounts,
            List<AdmissionDecision> decisions
    ) {
        Map<Long, CandidateState> unresolved = new LinkedHashMap<>();
        for (AdmissionCandidateInput candidate : tieGroup) {
            CandidateState state = new CandidateState(candidate);
            if (candidate.score().filingScore().compareTo(controlLine) < 0) {
                state.trace(null, null, AdmissionTraceAction.BELOW_CONTROL_LINE,
                        "投档分低于本科控制线" + controlLine.toPlainString());
                decisions.add(state.decide(
                        AdmissionResultStatus.BELOW_CONTROL_LINE, null, null, "未达到本科控制线"));
            } else {
                unresolved.put(candidate.candidateSnapshotId(), state);
            }
        }

        for (int preferenceNo = 1; preferenceNo <= 45 && !unresolved.isEmpty(); preferenceNo++) {
            Map<Long, List<CandidateState>> eligibleByPlan = new TreeMap<>();

            for (CandidateState state : unresolved.values()) {
                AdmissionPreferenceInput preference = preferenceAt(state.candidate, preferenceNo);
                if (preference == null) {
                    continue;
                }
                if (!preference.eligible()) {
                    state.trace(preference.planSnapshotId(), preferenceNo, AdmissionTraceAction.SKIPPED,
                            preference.invalidReason());
                    continue;
                }

                AdmissionPlanInput plan = plansById.get(preference.planSnapshotId());
                if (plan == null || plan.category() != state.candidate.category()) {
                    throw new IllegalStateException("志愿引用了不存在或跨科类的招生计划");
                }
                state.hasEligiblePreference = true;
                eligibleByPlan.computeIfAbsent(plan.planSnapshotId(), ignored -> new ArrayList<>()).add(state);
            }

            for (Map.Entry<Long, List<CandidateState>> entry : eligibleByPlan.entrySet()) {
                AdmissionPlanInput plan = plansById.get(entry.getKey());
                int currentCount = filedCounts.getOrDefault(plan.planSnapshotId(), 0);
                if (currentCount >= plan.capacity()) {
                    for (CandidateState state : entry.getValue()) {
                        state.trace(plan.planSnapshotId(), preferenceNo, AdmissionTraceAction.QUOTA_FULL,
                                "该院校专业组投档名额已满");
                    }
                    continue;
                }

                // 同分且志愿顺序相同者作为一个整体投档，允许末位同分突破计划名额。
                for (CandidateState state : entry.getValue()) {
                    state.trace(plan.planSnapshotId(), preferenceNo, AdmissionTraceAction.FILED,
                            "按第" + preferenceNo + "志愿投档");
                    decisions.add(state.decide(
                            AdmissionResultStatus.FILED,
                            plan.planSnapshotId(),
                            preferenceNo,
                            "已投档至第" + preferenceNo + "志愿"));
                    unresolved.remove(state.candidate.candidateSnapshotId());
                }
                filedCounts.put(plan.planSnapshotId(), currentCount + entry.getValue().size());
            }
        }

        for (CandidateState state : unresolved.values()) {
            if (state.hasEligiblePreference) {
                decisions.add(state.decide(
                        AdmissionResultStatus.SLIPPED, null, null, "所有有效志愿投档名额均已满"));
            } else {
                state.trace(null, null, AdmissionTraceAction.NO_VALID_PREFERENCE, "没有符合投档条件的志愿");
                decisions.add(state.decide(
                        AdmissionResultStatus.NO_VALID_PREFERENCE, null, null, "没有有效志愿"));
            }
        }
    }

    private Map<Long, AdmissionPlanInput> indexPlans(List<AdmissionPlanInput> plans) {
        Map<Long, AdmissionPlanInput> indexed = new HashMap<>();
        for (AdmissionPlanInput plan : plans) {
            if (indexed.put(plan.planSnapshotId(), plan) != null) {
                throw new IllegalArgumentException("招生计划快照ID重复: " + plan.planSnapshotId());
            }
        }
        return indexed;
    }

    private AdmissionPreferenceInput preferenceAt(AdmissionCandidateInput candidate, int preferenceNo) {
        return candidate.preferences().stream()
                .filter(preference -> preference.preferenceNo() == preferenceNo)
                .findFirst()
                .orElse(null);
    }

    private static final class CandidateState {
        private final AdmissionCandidateInput candidate;
        private final List<AdmissionTraceStep> traces = new ArrayList<>();
        private boolean hasEligiblePreference;

        private CandidateState(AdmissionCandidateInput candidate) {
            this.candidate = candidate;
        }

        private void trace(
                Long planSnapshotId,
                Integer preferenceNo,
                AdmissionTraceAction action,
                String detail
        ) {
            traces.add(new AdmissionTraceStep(
                    traces.size() + 1, planSnapshotId, preferenceNo, action, detail));
        }

        private AdmissionDecision decide(
                AdmissionResultStatus status,
                Long planSnapshotId,
                Integer preferenceNo,
                String reason
        ) {
            return new AdmissionDecision(
                    candidate.candidateSnapshotId(),
                    candidate.sourceCandidateId(),
                    status,
                    planSnapshotId,
                    preferenceNo,
                    reason,
                    traces
            );
        }
    }
}
