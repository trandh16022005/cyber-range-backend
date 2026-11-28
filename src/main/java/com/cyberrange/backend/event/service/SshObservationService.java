package com.cyberrange.backend.event.service;

import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.event.dto.CreateEventRequest;
import com.cyberrange.backend.event.repository.SshObservationRepository;
import com.cyberrange.backend.lab.repository.SshLabRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SshObservationService {

    public record Observation(String recordKey, Instant timestamp, String sourceIp, String username,
                              String outcome, String targetHost, String log) {}
    public record Receipt(String recordKey, long eventId, boolean duplicate) {}

    private static final Pattern RECORD_KEY = Pattern.compile("[A-Za-z0-9:_-]{1,160}");
    private static final Pattern ACCOUNT = Pattern.compile("[A-Za-z0-9_@.$-]{1,100}");
    private static final Pattern IPV4 = Pattern.compile("(0|[1-9][0-9]{0,2})(\\.(0|[1-9][0-9]{0,2})){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:.]{2,45}");
    private static final Duration DETECTION_LATENESS = Duration.ofMinutes(10);

    private final SshLabRepository labs;
    private final EventService events;
    private final SshObservationRepository observations;
    private final JsonMapper json;
    private final int threshold;
    private final Duration window;

    public SshObservationService(SshLabRepository labs, EventService events, SshObservationRepository observations,
                                 JsonMapper json, @Value("${ssh.detection.threshold:5}") int threshold,
                                 @Value("${ssh.detection.window-seconds:60}") long seconds) {
        if (threshold < 2 || threshold > 100 || seconds < 1 || seconds > 600) {
            throw new IllegalArgumentException("SSH detection configuration is outside supported bounds");
        }
        this.labs = labs;
        this.events = events;
        this.observations = observations;
        this.json = json;
        this.threshold = threshold;
        this.window = Duration.ofSeconds(seconds);
    }

    public void requireSession(UUID id) {
        labs.active().filter(session -> session.id().equals(id) && session.desiredRunning())
                .orElseThrow(() -> new ResourceNotFoundException("Active SSH Lab not found"));
    }

    @Transactional
    public List<Receipt> ingest(UUID id, List<Observation> batch) {
        if (batch == null || batch.isEmpty() || batch.size() > 64) {
            throw new IllegalArgumentException("SSH event batch must contain 1 to 64 observations");
        }
        var session = labs.lock(id).orElseThrow(() -> new ResourceNotFoundException("SSH Lab not found"));
        if ("STOPPED".equals(session.status())) {
            throw new BusinessException("SSH Lab is stopped");
        }
        return batch.stream().map(item -> ingestOne(session, item, Instant.now())).toList();
    }

    private Receipt ingestOne(SshLabRepository.Session session, Observation item, Instant now) {
        if (item == null || item.recordKey() == null || !RECORD_KEY.matcher(item.recordKey()).matches()) {
            throw new IllegalArgumentException("Invalid Collector record key");
        }
        var duplicate = observations.duplicate(session.id(), item.recordKey());
        if (duplicate.isPresent()) {
            return new Receipt(item.recordKey(), duplicate.get(), true);
        }
        if (item.timestamp() == null || item.timestamp().isAfter(now)
                || item.timestamp().isBefore(session.createdAt().minusSeconds(1))) {
            throw new IllegalArgumentException("Authentication timestamp is outside the active Lab session");
        }
        if (!"ubuntu-target".equals(item.targetHost()) || item.username() == null
                || !ACCOUNT.matcher(item.username()).matches() || item.log() == null
                || item.log().isBlank() || item.log().length() > 4096
                || item.log().codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid SSH authentication log payload");
        }
        boolean failure = "FAILURE".equals(item.outcome());
        if (!failure && !"SUCCESS".equals(item.outcome())) {
            throw new IllegalArgumentException("Invalid authentication outcome");
        }
        String ip = literalAddress(item.sourceIp());
        var data = json.createObjectNode();
        data.put("sourceIp", ip);
        data.put("username", item.username());
        data.put("authenticationOutcome", item.outcome());
        data.put("targetHost", item.targetHost());
        data.put("raw", item.log());
        data.put("provenance", "OBSERVED_LAB");
        Instant eventTime = item.timestamp().truncatedTo(ChronoUnit.MICROS);
        var event = events.createEvent(new CreateEventRequest(session.exerciseId(), session.assetId(), session.serviceId(),
                failure ? "SSH_AUTH_FAILURE" : "SSH_AUTH_SUCCESS", failure ? "MEDIUM" : "INFO",
                eventTime, "OPENSSH_COLLECTOR", data));
        observations.markObserved(event.id(), session.id(), item.recordKey());

        if (failure && !item.timestamp().isBefore(now.minus(DETECTION_LATENESS))) {
            detect(session, item.username(), ip, event.id(), eventTime, now);
        }
        return new Receipt(item.recordKey(), event.id(), false);
    }

    private void detect(SshLabRepository.Session session, String account, String ip, long eventId, Instant eventTime, Instant now) {
        var incoming = new SshDetectionWindow.Failure(eventId, eventTime);
        var failures = observations.failures(session.id(), session.exerciseId(), session.serviceId(), ip, account,
                        eventTime.minus(window), eventTime.plus(window)).stream()
                .map(failure -> new SshDetectionWindow.Failure(failure.id(), failure.at())).toList();
        SshDetectionWindow.matching(failures, incoming, threshold, window).ifPresent(matching -> {
            long alertId = observations.findOrCreateAlert(session.id(), session.exerciseId(), session.serviceId(), ip,
                    account, matching.getFirst().at(), matching.getLast().at(), now);
            matching.forEach(failure -> observations.link(alertId, failure.id()));
        });
    }

    private static String literalAddress(String value) {
        if (value != null && (IPV4.matcher(value).matches()
                || (value.indexOf(':') >= 0 && IPV6.matcher(value).matches()))) {
            try {
                return InetAddress.getByName(value).getHostAddress();
            } catch (UnknownHostException ignored) {
                // The shape matched an address, but one or more numeric components are invalid.
            }
        }
        throw new IllegalArgumentException("Source IP must be a valid IPv4 or IPv6 literal");
    }
}
