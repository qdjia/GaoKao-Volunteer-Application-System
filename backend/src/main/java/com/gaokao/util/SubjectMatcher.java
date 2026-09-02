package com.gaokao.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SubjectMatcher {

    public static final String PHYSICS = "物理";
    public static final String HISTORY = "历史";

    private static final List<String> OPTIONAL_SUBJECTS = List.of("化学", "生物", "政治", "地理");

    private SubjectMatcher() {
    }

    public static String primarySubject(String subjectCombo) {
        Set<String> subjects = normalize(subjectCombo);
        if (subjects.contains(PHYSICS)) {
            return PHYSICS;
        }
        if (subjects.contains(HISTORY)) {
            return HISTORY;
        }
        return null;
    }

    public static boolean isSubjectMatch(String subjectCombo, String requirement) {
        Set<String> studentSubjects = normalize(subjectCombo);
        if (!isValidCombination(subjectCombo)) {
            return false;
        }
        Set<String> requiredSubjects = normalizeRequirement(requirement);
        if (requiredSubjects.isEmpty()) {
            return true;
        }

        String requiredPrimary = requiredSubjects.contains(PHYSICS) ? PHYSICS
                : requiredSubjects.contains(HISTORY) ? HISTORY : null;
        String studentPrimary = primarySubject(subjectCombo);
        if (requiredPrimary != null && !requiredPrimary.equals(studentPrimary)) {
            return false;
        }

        return studentSubjects.containsAll(requiredSubjects);
    }

    public static boolean isMajorMatch(String subjectCombo, String subjectType, String requirement) {
        String studentPrimary = primarySubject(subjectCombo);
        if (subjectType != null && !subjectType.isBlank() && !subjectType.equals(studentPrimary)) {
            return false;
        }
        return isSubjectMatch(subjectCombo, requirement);
    }

    public static Set<String> normalize(String text) {
        Set<String> subjects = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return subjects;
        }

        boolean compact = !containsFullSubjectName(text);
        collectSubject(text, subjects, compact, "物理", "物");
        collectSubject(text, subjects, compact, "历史", "史");
        collectSubject(text, subjects, compact, "化学", "化");
        collectSubject(text, subjects, compact, "生物", "生");
        collectSubject(text, subjects, compact, "政治", "政");
        collectSubject(text, subjects, compact, "地理", "地");

        return subjects;
    }

    public static boolean isValidCombination(String subjectCombo) {
        Set<String> subjects = normalize(subjectCombo);
        boolean hasPhysics = subjects.contains(PHYSICS);
        boolean hasHistory = subjects.contains(HISTORY);
        long optionalCount = OPTIONAL_SUBJECTS.stream().filter(subjects::contains).count();
        return hasPhysics != hasHistory && optionalCount == 2 && subjects.size() == 3;
    }

    private static Set<String> normalizeRequirement(String requirement) {
        Set<String> requiredSubjects = new LinkedHashSet<>();
        if (requirement == null || requirement.isBlank()) {
            return requiredSubjects;
        }

        boolean compact = !containsFullSubjectName(requirement);
        collectSubject(requirement, requiredSubjects, compact, "物理", "物");
        collectSubject(requirement, requiredSubjects, compact, "历史", "史");
        collectSubject(requirement, requiredSubjects, compact, "化学", "化");
        collectSubject(requirement, requiredSubjects, compact, "生物", "生");
        collectSubject(requirement, requiredSubjects, compact, "政治", "政");
        collectSubject(requirement, requiredSubjects, compact, "地理", "地");
        return requiredSubjects;
    }

    private static void collectSubject(String text, Set<String> subjects, boolean compact, String fullName, String shortName) {
        if (text.contains(fullName) || (compact && text.contains(shortName))) {
            subjects.add(fullName);
        }
    }

    private static boolean containsFullSubjectName(String text) {
        return text.contains("物理") || text.contains("历史") || text.contains("化学")
                || text.contains("生物") || text.contains("政治") || text.contains("地理");
    }

}
