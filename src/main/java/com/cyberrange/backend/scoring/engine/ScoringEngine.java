package com.cyberrange.backend.scoring.engine;

import com.cyberrange.backend.common.exception.BusinessException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class ScoringEngine {

    private static final String RULE_TYPE = "EVIDENCE_FIELD_MATCH";

    private ScoringEngine() {}

    public static BigDecimal normalize(BigDecimal earned, BigDecimal applicableMaximum) {
        return applicableMaximum.signum() == 0 ? null
                : earned.multiply(BigDecimal.valueOf(100)).divide(applicableMaximum, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal setMatch(Set<Long> selected, Set<Long> reference) {
        if (reference.isEmpty()) {
            throw new IllegalArgumentException("The reference set must not be empty");
        }
        var union = new HashSet<>(reference);
        union.addAll(selected);
        long correct = selected.stream().filter(reference::contains).count();
        return BigDecimal.valueOf(correct).divide(BigDecimal.valueOf(union.size()), 12, RoundingMode.HALF_UP);
    }

    public static BigDecimal evidenceRatio(String type, JsonNode data, Map<String, Object> rule) {
        if (!rule.get("evidenceType").equals(type) || data == null || !data.isObject()) {
            return BigDecimal.ZERO;
        }
        if ("SET_MATCH".equals(rule.get("ruleType"))) {
            var selected = ids(data.get((String) rule.get("selectedField")));
            var reference = ids(data.get((String) rule.get("referenceField")));
            return selected == null || reference == null || reference.isEmpty()
                    ? BigDecimal.ZERO : setMatch(selected, reference);
        }
        if ("TIME_THRESHOLD".equals(rule.get("ruleType"))) {
            return "AUTOMATED_MATCH".equals(timeOutcome(data, rule)) ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return matchesEvidence(type, data, rule) ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    public static String timeOutcome(JsonNode data, Map<String, Object> rule) {
        var elapsed = data == null ? null : data.get((String) rule.get("durationField"));
        if (elapsed == null || elapsed.isNull()) {
            return "TIME_NOT_MEASURED";
        }
        if (!elapsed.isNumber() || elapsed.decimalValue().signum() < 0) {
            return "TIME_INVALID";
        }
        return elapsed.decimalValue().compareTo(toBigDecimal(rule.get("maximumSeconds"))) <= 0
                ? "AUTOMATED_MATCH" : "TIME_THRESHOLD_EXCEEDED";
    }

    private static Set<Long> ids(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        var result = new HashSet<Long>();
        for (JsonNode item : node) {
            if (!item.isIntegralNumber() || !item.canConvertToLong() || item.asLong() <= 0) {
                return null;
            }
            result.add(item.asLong());
        }
        return result;
    }

    public static boolean matchesEvidence(String type, JsonNode data, Map<String, Object> rule) {
        Object expectedEvidenceType = rule.get("evidenceType");

        if (!(expectedEvidenceType instanceof String evidenceType)
                || !evidenceType.equals(type)) {
            return false;
        }



        if (data == null || !data.isObject()) {
            return false;
        }

        Object rawConditions = rule.get("conditions");

        if (!(rawConditions instanceof Map<?, ?> conditions)
                || conditions.isEmpty()) {
            return false;
        }

        // Every configured condition must match.
        for (Map.Entry<?, ?> condition : conditions.entrySet()) {

            if (!(condition.getKey() instanceof String fieldName)) {
                return false;
            }

            JsonNode actualValue = data.get(fieldName);

            if (actualValue == null
                    || actualValue.isNull()
                    || !jsonValueMatches(
                    actualValue,
                    condition.getValue()
            )) {
                return false;
            }
        }

        return true;
    }

    private static boolean jsonValueMatches(
            JsonNode actual,
            Object expected
    ) {
        if (expected instanceof Number number) {
            if (!actual.isNumber()) {
                return false;
            }

            return actual.decimalValue().compareTo(
                    new BigDecimal(number.toString())
            ) == 0;
        }

        if (expected instanceof Boolean bool) {
            return actual.isBoolean()
                    && actual.booleanValue() == bool;
        }

        if (expected instanceof String text) {
            return actual.isString()
                    && actual.stringValue().equals(text);
        }

        return false;
    }

    public static void validateRule(Map<String, Object> rule) {

        boolean setMatch = "SET_MATCH".equals(rule.get("ruleType"));
        boolean time = "TIME_THRESHOLD".equals(rule.get("ruleType"));
        if (!RULE_TYPE.equals(rule.get("ruleType")) && !setMatch && !time) {
            throw new BusinessException(
                    "Unsupported scoring rule type"
            );
        }

        if (!(rule.get("evidenceType") instanceof String evidenceType)
                || evidenceType.isBlank()) {
            throw new BusinessException(
                    "Scoring rule must define evidenceType"
            );
        }

        if (setMatch) {
            if (!(rule.get("selectedField") instanceof String selected) || selected.isBlank()
                    || !(rule.get("referenceField") instanceof String reference) || reference.isBlank()
                    || selected.equals(reference)) {
                throw new BusinessException("SET_MATCH must define distinct selectedField and referenceField");
            }
        } else if (time) {
            if (!(rule.get("durationField") instanceof String field) || field.isBlank()
                    || toBigDecimal(rule.get("maximumSeconds")).signum() < 0) {
                throw new BusinessException("TIME_THRESHOLD requires durationField and non-negative maximumSeconds");
            }
        } else if (!(rule.get("conditions") instanceof Map<?, ?> conditions) || conditions.isEmpty()) {
            throw new BusinessException(
                    "Scoring rule must define non-empty conditions"
            );
        }

        BigDecimal score = toBigDecimal(rule.get("score"));

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "Scoring rule score cannot be negative"
            );
        }
    }

    public static BigDecimal toBigDecimal(Object value) {

        if (!(value instanceof Number)
                && !(value instanceof String)) {
            throw new BusinessException(
                    "Scoring rule score must be numeric"
            );
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    "Invalid scoring rule score"
            );
        }
    }

}
