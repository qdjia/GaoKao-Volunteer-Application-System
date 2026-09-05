package com.gaokao.admission;

import com.gaokao.domain.AdmissionResultStatus;
import com.gaokao.domain.SecondarySubject;
import com.gaokao.domain.SubjectCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdmissionRunService {
    private final AdmissionRunStore store;
    private final FilingCapacityPolicy capacityPolicy;
    private final ParallelAdmissionEngine engine;

    public AdmissionRunService(
            AdmissionRunStore store,
            FilingCapacityPolicy capacityPolicy,
            ParallelAdmissionEngine engine
    ) {
        this.store = store;
        this.capacityPolicy = capacityPolicy;
        this.engine = engine;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AdmissionRunSummary execute(long batchId, long operatorUserId) {
        AdmissionRunSource.Batch batch = store.lockBatch(batchId);
        if (!"CLOSED".equals(batch.status())) {
            throw new IllegalStateException("只有已截止的招生批次可以执行投档");
        }

        List<AdmissionRunSource.ControlLine> controlLines = store.loadControlLines(batchId);
        validateControlLines(controlLines);
        List<AdmissionRunSource.Plan> plans = store.loadPlans(batchId);
        if (plans.isEmpty()) {
            throw new IllegalStateException("当前批次没有招生计划");
        }
        List<AdmissionRunSource.Candidate> candidates = store.loadCandidates(batchId, batch.examYearId());

        int runNo = store.nextRunNumber(batchId);
        long runId = store.createRun(batchId, batch.examYearId(), runNo, operatorUserId);

        Map<SubjectCategory, BigDecimal> snapshotControlLines = new EnumMap<>(SubjectCategory.class);
        for (AdmissionRunSource.ControlLine controlLine : controlLines) {
            store.snapshotControlLine(runId, controlLine);
            snapshotControlLines.put(controlLine.category(), controlLine.score());
        }

        Map<Long, AdmissionRunSource.Plan> sourcePlans = new HashMap<>();
        Map<Long, Long> planSnapshotIds = new HashMap<>();
        List<AdmissionPlanInput> planInputs = new ArrayList<>();
        for (AdmissionRunSource.Plan plan : plans) {
            int capacity = capacityPolicy.calculate(plan.plannedCount(), plan.filingRatio());
            long snapshotId = store.snapshotPlan(runId, plan, capacity);
            sourcePlans.put(plan.id(), plan);
            planSnapshotIds.put(plan.id(), snapshotId);
            planInputs.add(new AdmissionPlanInput(snapshotId, plan.category(), capacity));
        }

        Map<Long, Long> sortOrders = calculateSortOrders(candidates);
        List<AdmissionCandidateInput> candidateInputs = new ArrayList<>();
        for (AdmissionRunSource.Candidate candidate : candidates) {
            long candidateSnapshotId = store.snapshotCandidate(
                    runId, candidate, sortOrders.get(candidate.id()));
            List<AdmissionPreferenceInput> preferenceInputs = new ArrayList<>();

            for (AdmissionRunSource.Preference preference : candidate.preferences()) {
                AdmissionRunSource.Plan sourcePlan = sourcePlans.get(preference.planId());
                Long planSnapshotId = planSnapshotIds.get(preference.planId());
                if (sourcePlan == null || planSnapshotId == null) {
                    throw new IllegalStateException("志愿引用的招生计划不属于当前批次");
                }

                String invalidReason = invalidReason(candidate, sourcePlan);
                boolean eligible = invalidReason == null;
                long preferenceSnapshotId = store.snapshotPreference(
                        runId,
                        candidateSnapshotId,
                        planSnapshotId,
                        preference,
                        eligible,
                        invalidReason);
                for (AdmissionRunSource.MajorChoice major : preference.majorChoices()) {
                    store.snapshotMajorChoice(runId, preferenceSnapshotId, major);
                }
                preferenceInputs.add(new AdmissionPreferenceInput(
                        preferenceSnapshotId,
                        planSnapshotId,
                        preference.preferenceNo(),
                        eligible,
                        invalidReason));
            }

            candidateInputs.add(new AdmissionCandidateInput(
                    candidateSnapshotId,
                    candidate.id(),
                    candidate.category(),
                    candidate.admissionScore(),
                    preferenceInputs));
        }

        List<AdmissionDecision> decisions = engine.execute(
                candidateInputs, planInputs, snapshotControlLines);
        for (AdmissionDecision decision : decisions) {
            store.saveDecision(runId, decision);
        }
        store.completeRun(runId);
        return summarize(runId, runNo, decisions);
    }

    @Transactional(readOnly = true)
    public List<AdmissionResultView> findResults(long runId) {
        return store.findResults(runId);
    }

    @Transactional(readOnly = true)
    public List<AdmissionTraceView> findTraces(long runId, long candidateId) {
        return store.findTraces(runId, candidateId);
    }

    private void validateControlLines(List<AdmissionRunSource.ControlLine> controlLines) {
        Set<SubjectCategory> categories = controlLines.stream()
                .map(AdmissionRunSource.ControlLine::category)
                .collect(java.util.stream.Collectors.toSet());
        if (!categories.equals(Set.of(SubjectCategory.PHYSICS, SubjectCategory.HISTORY))) {
            throw new IllegalStateException("投档前必须配置物理类和历史类控制线");
        }
    }

    private String invalidReason(
            AdmissionRunSource.Candidate candidate,
            AdmissionRunSource.Plan plan
    ) {
        if (candidate.category() != plan.category()) {
            return "首选科目与院校专业组科类不匹配";
        }
        if (!plan.active()) {
            return "院校或院校专业组已停用";
        }
        if (!candidate.secondarySubjects().containsAll(plan.requiredSubjects())) {
            String required = plan.requiredSubjects().stream()
                    .map(SecondarySubject::name)
                    .sorted()
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            return "再选科目不满足要求: " + required;
        }
        return null;
    }

    private Map<Long, Long> calculateSortOrders(List<AdmissionRunSource.Candidate> candidates) {
        Map<Long, Long> result = new HashMap<>();
        Comparator<AdmissionRunSource.Candidate> comparator = Comparator
                .comparing(AdmissionRunSource.Candidate::admissionScore, AdmissionScore.DESCENDING);

        for (SubjectCategory category : SubjectCategory.values()) {
            List<AdmissionRunSource.Candidate> queue = candidates.stream()
                    .filter(candidate -> candidate.category() == category)
                    .sorted(comparator)
                    .toList();
            long rank = 1;
            int offset = 0;
            while (offset < queue.size()) {
                int end = offset + 1;
                while (end < queue.size()
                        && AdmissionScore.DESCENDING.compare(
                        queue.get(offset).admissionScore(), queue.get(end).admissionScore()) == 0) {
                    end++;
                }
                for (int index = offset; index < end; index++) {
                    result.put(queue.get(index).id(), rank);
                }
                rank += end - offset;
                offset = end;
            }
        }
        return result;
    }

    private AdmissionRunSummary summarize(
            long runId,
            int runNo,
            List<AdmissionDecision> decisions
    ) {
        return new AdmissionRunSummary(
                runId,
                runNo,
                decisions.size(),
                count(decisions, AdmissionResultStatus.FILED),
                count(decisions, AdmissionResultStatus.SLIPPED),
                count(decisions, AdmissionResultStatus.BELOW_CONTROL_LINE),
                count(decisions, AdmissionResultStatus.NO_VALID_PREFERENCE)
        );
    }

    private int count(List<AdmissionDecision> decisions, AdmissionResultStatus status) {
        return (int) decisions.stream().filter(decision -> decision.status() == status).count();
    }
}
