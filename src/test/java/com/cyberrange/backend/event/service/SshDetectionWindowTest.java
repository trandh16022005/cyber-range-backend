package com.cyberrange.backend.event.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SshDetectionWindowTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration WINDOW = Duration.ofSeconds(60);

    @Test
    void recognizesAnOutOfOrderFailureAtTheInclusiveBoundary() {
        var incoming = failure(3, 0);
        var window = SshDetectionWindow.matching(List.of(failure(1, 60), failure(2, 24), incoming), incoming, 3, WINDOW);
        assertEquals(List.of(incoming, failure(2, 24), failure(1, 60)), window.orElseThrow());
    }

    @Test
    void requiresThresholdInTheSameWindow() {
        var incoming = failure(3, 61);
        assertTrue(SshDetectionWindow.matching(List.of(failure(1, 0), failure(2, 14), incoming), incoming, 3, WINDOW).isEmpty());
    }

    @Test
    void ignoresExistingWindowThatDoesNotContainIncomingEvent() {
        var incoming = failure(4, 75);
        assertTrue(SshDetectionWindow.matching(List.of(failure(1, 0), failure(2, 1), failure(3, 2), incoming), incoming, 3, WINDOW).isEmpty());
    }

    private SshDetectionWindow.Failure failure(long id, long seconds) {
        return new SshDetectionWindow.Failure(id, START.plusSeconds(seconds));
    }
}
