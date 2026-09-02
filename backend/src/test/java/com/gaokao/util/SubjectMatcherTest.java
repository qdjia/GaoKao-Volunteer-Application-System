package com.gaokao.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectMatcherTest {

    @Test
    void keepsPhysicsAndHistoryPlansIndependentEvenWithoutExtraRequirements() {
        assertTrue(SubjectMatcher.isMajorMatch("物化生", "物理", null));
        assertFalse(SubjectMatcher.isMajorMatch("史政地", "物理", null));
        assertTrue(SubjectMatcher.isMajorMatch("史政地", "历史", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "物化生", "物化政", "物化地", "物生政", "物生地", "物政地",
            "史化生", "史化政", "史化地", "史生政", "史生地", "史政地"
    })
    void acceptsEveryValidThreeSubjectCombination(String combination) {
        assertEquals(3, SubjectMatcher.normalize(combination).size());
        assertTrue(SubjectMatcher.isValidCombination(combination));
        assertTrue(SubjectMatcher.isSubjectMatch(combination, combination));
    }

    @ParameterizedTest
    @ValueSource(strings = {"物理", "历史", "物化", "史政"})
    void rejectsIncompleteCombinationsInsteadOfInventingSubjects(String combination) {
        assertFalse(SubjectMatcher.isValidCombination(combination));
        assertFalse(SubjectMatcher.isSubjectMatch(combination, "化学"));
    }
}
