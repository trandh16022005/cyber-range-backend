package com.cyberrange.backend.lab.service;

import com.cyberrange.backend.assessment.repository.AssessmentRepository;
import com.cyberrange.backend.assessment.service.SshAssessmentEvaluator;
import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.exercise.repository.ExerciseParticipantRepository;
import com.cyberrange.backend.exercise.repository.ExerciseRepository;
import com.cyberrange.backend.lab.repository.SshLabRepository;
import com.cyberrange.backend.lab.repository.SshLabRepository.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class SshLabService {

    public record Health(boolean target, boolean client, boolean collector, boolean ssh, boolean logging,
                         boolean collectorBackend, String clientAddress, String error) {
        boolean ready() {
            return target && client && collector && ssh && logging && collectorBackend;
        }
    }

    public record Status(UUID id, long exerciseId, long assessmentId, String status,
                         boolean ready, boolean desiredRunning, boolean backend, Instant heartbeatAt, JsonNode health) {}

    private final SshLabRepository labs;
    private final ExerciseRepository exercises;
    private final AssessmentRepository assessments;
    private final ExerciseParticipantRepository participants;
    private final JsonMapper json;
    private volatile boolean acceptingStarts = true;

    public SshLabService(SshLabRepository labs, ExerciseRepository exercises, AssessmentRepository assessments,
                         ExerciseParticipantRepository participants, JsonMapper json) {
        this.labs = labs;
        this.exercises = exercises;
        this.assessments = assessments;
        this.participants = participants;
        this.json = json;
    }

    @Transactional
    public Status start(long exerciseId, long assessmentId) {
        labs.lockLifecycle();
        if (!acceptingStarts) {
            throw new BusinessException("The managed Lab runner is not available to start a session");
        }
        var exercise = exercises.findByIdForUpdate(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));
        if (!Set.of("CREATED", "READY", "RUNNING", "PAUSED").contains(exercise.getStatus())
                || !"PUBLISHED".equals(exercise.getScenario().getStatus())
                || !SshAssessmentEvaluator.SCENARIO_NAME.equals(exercise.getScenario().getName())) {
            throw new BusinessException("Exercise is not eligible for the SSH Lab");
        }
        var assessment = assessments.findWithExerciseAndTeamById(assessmentId)
                .filter(value -> value.getExercise().getId().equals(exerciseId))
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found for this Exercise"));
        boolean blue = participants.findByExercise_IdOrderByJoinedAtAsc(exerciseId).stream()
                .anyMatch(member -> "BLUE".equals(member.getRole()) && member.getTeam() != null
                        && member.getTeam().getId().equals(assessment.getTeam().getId()));
        if (!blue) {
            throw new BusinessException("Assessment Team is not registered as BLUE for this Exercise");
        }
        var active = labs.active();
        if (active.isPresent()) {
            Session session = active.get();
            if (session.exerciseId() == exerciseId && session.assessmentId() == assessmentId && session.desiredRunning()) {
                return toStatus(session);
            }
            throw new BusinessException("An SSH Lab is already active");
        }

        var target = labs.resolveTarget(exerciseId)
                .orElseThrow(() -> new BusinessException("Configure the Ubuntu Target and its SSH Service before starting"));
        labs.insert(UUID.randomUUID(), exerciseId, assessmentId, target);
        return labs.latest(exerciseId).map(SshLabService::toStatus).orElseThrow();
    }

    @Transactional
    public Status stop(long exerciseId) {
        labs.lockLifecycle();
        var session = labs.latest(exerciseId).orElseThrow(() -> new ResourceNotFoundException("SSH Lab not found"));
        if (!"STOPPED".equals(session.status())) {
            labs.requestStop(session.id());
        }
        return labs.latest(exerciseId).map(SshLabService::toStatus).orElseThrow();
    }

    public boolean isStopped(UUID sessionId) {
        return labs.find(sessionId).map(session -> "STOPPED".equals(session.status())).orElse(false);
    }

    public Status status(long exerciseId) {
        if (!exercises.existsById(exerciseId)) {
            throw new ResourceNotFoundException("Exercise not found");
        }
        return labs.latest(exerciseId).map(SshLabService::toStatus).orElse(null);
    }

    public Session desired() {
        return labs.active().orElse(null);
    }

    public void allowStarts(boolean allowed) {
        acceptingStarts = allowed;
    }

    @Transactional
    public UUID requestManagedStop() {
        labs.lockLifecycle();
        var session = labs.active().orElse(null);
        if (session == null) {
            return null;
        }
        if (session.desiredRunning()) {
            labs.requestStop(session.id());
        }
        return session.id();
    }

    @Transactional
    public void heartbeat(UUID id, String status, Health health) {
        if (status == null || !Set.of("STARTING", "RUNNING", "STOPPED", "ERROR").contains(status) || health == null
                || (health.error() != null && health.error().length() > 240)) {
            throw new IllegalArgumentException("Invalid Lab heartbeat");
        }
        var session = labs.lock(id).orElseThrow(() -> new ResourceNotFoundException("SSH Lab not found"));
        if ("STOPPED".equals(session.status())) {
            return;
        }
        if ("STOPPED".equals(status) && session.desiredRunning()) {
            throw new BusinessException("Stop was not requested for this Lab");
        }
        labs.recordHealth(id, status, json.valueToTree(health));
    }

    private static Status toStatus(Session session) {
        boolean fresh = session.heartbeatAt() != null && session.heartbeatAt().isAfter(Instant.now().minusSeconds(30));
        boolean stale = !"STOPPED".equals(session.status()) && !fresh
                && session.createdAt().isBefore(Instant.now().minusSeconds(30));
        var health = session.health();
        boolean ready = session.desiredRunning() && fresh && "RUNNING".equals(session.status()) && health != null
                && health.path("target").asBoolean() && health.path("client").asBoolean()
                && health.path("collector").asBoolean() && health.path("ssh").asBoolean()
                && health.path("logging").asBoolean() && health.path("collectorBackend").asBoolean();
        return new Status(session.id(), session.exerciseId(), session.assessmentId(),
                stale ? "ERROR" : session.status(), ready, session.desiredRunning(), true, session.heartbeatAt(), stale ? null : health);
    }
}
