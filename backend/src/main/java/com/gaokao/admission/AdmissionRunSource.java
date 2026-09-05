package com.gaokao.admission;

import com.gaokao.domain.SecondarySubject;
import com.gaokao.domain.SubjectCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public final class AdmissionRunSource {
    private AdmissionRunSource() {
    }

    public record Batch(long id, long examYearId, String status) {
    }

    public record ControlLine(long id, SubjectCategory category, BigDecimal score) {
    }

    public record Plan(
            long id,
            SubjectCategory category,
            String institutionCode,
            String institutionName,
            String groupCode,
            String groupName,
            boolean active,
            Set<SecondarySubject> requiredSubjects,
            int plannedCount,
            BigDecimal filingRatio
    ) {
        public Plan {
            requiredSubjects = Set.copyOf(requiredSubjects);
        }
    }

    public record Candidate(
            long id,
            Long submissionId,
            Integer submissionVersion,
            String examNumber,
            String name,
            SubjectCategory category,
            SecondarySubject secondarySubject1,
            SecondarySubject secondarySubject2,
            BigDecimal chineseScore,
            BigDecimal mathematicsScore,
            BigDecimal foreignLanguageScore,
            BigDecimal primarySubjectScore,
            BigDecimal secondarySubject1Score,
            BigDecimal secondarySubject2Score,
            BigDecimal policyBonus,
            BigDecimal cultureTotal,
            int finalRank,
            List<Preference> preferences
    ) {
        public Candidate {
            preferences = List.copyOf(preferences);
        }

        public Set<SecondarySubject> secondarySubjects() {
            return Set.of(secondarySubject1, secondarySubject2);
        }

        public AdmissionScore admissionScore() {
            return AdmissionScore.of(
                    cultureTotal,
                    policyBonus,
                    chineseScore,
                    mathematicsScore,
                    foreignLanguageScore,
                    primarySubjectScore,
                    secondarySubject1Score,
                    secondarySubject2Score
            );
        }
    }

    public record Preference(
            long id,
            long planId,
            int preferenceNo,
            boolean acceptAdjustment,
            List<MajorChoice> majorChoices
    ) {
        public Preference {
            majorChoices = List.copyOf(majorChoices);
        }
    }

    public record MajorChoice(
            long id,
            int preferenceNo,
            String majorCode,
            String majorName,
            String warningMessage
    ) {
    }
}
