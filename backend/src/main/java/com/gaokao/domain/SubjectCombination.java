package com.gaokao.domain;

import java.util.Set;

public enum SubjectCombination {
    PHYSICS_CHEMISTRY_BIOLOGY(SubjectCategory.PHYSICS, SecondarySubject.CHEMISTRY, SecondarySubject.BIOLOGY),
    PHYSICS_CHEMISTRY_POLITICS(SubjectCategory.PHYSICS, SecondarySubject.CHEMISTRY, SecondarySubject.POLITICS),
    PHYSICS_CHEMISTRY_GEOGRAPHY(SubjectCategory.PHYSICS, SecondarySubject.CHEMISTRY, SecondarySubject.GEOGRAPHY),
    PHYSICS_BIOLOGY_POLITICS(SubjectCategory.PHYSICS, SecondarySubject.BIOLOGY, SecondarySubject.POLITICS),
    PHYSICS_BIOLOGY_GEOGRAPHY(SubjectCategory.PHYSICS, SecondarySubject.BIOLOGY, SecondarySubject.GEOGRAPHY),
    PHYSICS_POLITICS_GEOGRAPHY(SubjectCategory.PHYSICS, SecondarySubject.POLITICS, SecondarySubject.GEOGRAPHY),
    HISTORY_CHEMISTRY_BIOLOGY(SubjectCategory.HISTORY, SecondarySubject.CHEMISTRY, SecondarySubject.BIOLOGY),
    HISTORY_CHEMISTRY_POLITICS(SubjectCategory.HISTORY, SecondarySubject.CHEMISTRY, SecondarySubject.POLITICS),
    HISTORY_CHEMISTRY_GEOGRAPHY(SubjectCategory.HISTORY, SecondarySubject.CHEMISTRY, SecondarySubject.GEOGRAPHY),
    HISTORY_BIOLOGY_POLITICS(SubjectCategory.HISTORY, SecondarySubject.BIOLOGY, SecondarySubject.POLITICS),
    HISTORY_BIOLOGY_GEOGRAPHY(SubjectCategory.HISTORY, SecondarySubject.BIOLOGY, SecondarySubject.GEOGRAPHY),
    HISTORY_POLITICS_GEOGRAPHY(SubjectCategory.HISTORY, SecondarySubject.POLITICS, SecondarySubject.GEOGRAPHY);

    private final SubjectCategory category;
    private final Set<SecondarySubject> secondarySubjects;

    SubjectCombination(
            SubjectCategory category,
            SecondarySubject secondarySubject1,
            SecondarySubject secondarySubject2
    ) {
        this.category = category;
        this.secondarySubjects = Set.of(secondarySubject1, secondarySubject2);
    }

    public SubjectCategory category() {
        return category;
    }

    public Set<SecondarySubject> secondarySubjects() {
        return secondarySubjects;
    }
}
