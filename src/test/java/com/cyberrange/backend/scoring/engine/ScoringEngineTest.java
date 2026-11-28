package com.cyberrange.backend.scoring.engine;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {
    @Test
    void distinguishesZeroMissingInvalidAndExceededTime() {
        var rule = Map.<String, Object>of("ruleType", "TIME_THRESHOLD", "evidenceType", "MEASURED",
                "durationField", "seconds", "maximumSeconds", 60, "score", 2);
        ScoringEngine.validateRule(rule);
        var json = JsonMapper.builder().build();
        assertEquals("TIME_NOT_MEASURED", ScoringEngine.timeOutcome(json.readTree("{}"), rule));
        assertEquals("TIME_INVALID", ScoringEngine.timeOutcome(json.readTree("{\"seconds\":-1}"), rule));
        assertEquals("AUTOMATED_MATCH", ScoringEngine.timeOutcome(json.readTree("{\"seconds\":0}"), rule));
        assertEquals("AUTOMATED_MATCH", ScoringEngine.timeOutcome(json.readTree("{\"seconds\":60}"), rule));
        assertEquals("TIME_THRESHOLD_EXCEEDED", ScoringEngine.timeOutcome(json.readTree("{\"seconds\":60.001}"), rule));
    }
    @Test
    void setMatchPenalizesWrongAndMissingSelectionsAndIgnoresDuplicates() {
        var rule = Map.<String, Object>of("ruleType", "SET_MATCH", "evidenceType", "SELECTION",
                "selectedField", "selected", "referenceField", "reference", "score", 10);
        ScoringEngine.validateRule(rule);
        var json = JsonMapper.builder().build();
        var evidence = json.readTree("""
                {"selected":[1,1,3],"reference":[1,2,4]}
                """);
        assertEquals(0, new BigDecimal("0.25").compareTo(ScoringEngine.evidenceRatio("SELECTION", evidence, rule)));
        assertEquals(BigDecimal.ZERO, ScoringEngine.evidenceRatio("OTHER", evidence, rule));
        assertEquals(BigDecimal.ZERO, ScoringEngine.evidenceRatio("SELECTION",
                json.readTree("{\"selected\":[1],\"reference\":[]}"), rule));
    }

    @Test
    void normalizesApplicablePointsAndHandlesZeroMaximum() {
        assertEquals(new BigDecimal("83.33"), ScoringEngine.normalize(BigDecimal.valueOf(75), BigDecimal.valueOf(90)));
        assertNull(ScoringEngine.normalize(BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
