package com.cyberrange.backend.event.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class SshDetectionWindow {

    record Failure(long id, Instant at) {}

    private SshDetectionWindow() {}

    static Optional<List<Failure>> matching(List<Failure> failures, Failure incoming, int threshold, Duration window) {
        List<Failure> ordered = failures.stream()
                .sorted(Comparator.comparing(Failure::at).thenComparingLong(Failure::id))
                .toList();
        for (int first = 0, last = 0; last < ordered.size(); last++) {
            Instant end = ordered.get(last).at();
            if (end.isBefore(incoming.at())) {
                continue;
            }
            Instant cutoff = end.minus(window);
            if (cutoff.isAfter(incoming.at())) {
                break;
            }
            while (ordered.get(first).at().isBefore(cutoff)) {
                first++;
            }
            if (last - first + 1 >= threshold
                    && (ordered.get(last).at().isAfter(incoming.at()) || ordered.get(last).id() >= incoming.id())) {
                return Optional.of(ordered.subList(first, last + 1));
            }
        }
        return Optional.empty();
    }
}
