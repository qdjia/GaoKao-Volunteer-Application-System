package com.gaokao.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectCombinationTest {

    @Test
    void exposesSixCombinationsForEachSubjectCategory() {
        assertThat(SubjectCombination.values()).hasSize(12);

        for (SubjectCategory category : SubjectCategory.values()) {
            long count = Arrays.stream(SubjectCombination.values())
                    .filter(combination -> combination.category() == category)
                    .count();
            assertThat(count).isEqualTo(6);
        }
    }

    @Test
    void everyCombinationContainsTwoDistinctSecondarySubjects() {
        for (SubjectCombination combination : SubjectCombination.values()) {
            assertThat(combination.secondarySubjects()).hasSize(2);
        }
    }
}
