package com.cyberrange.backend.lab.dto;

import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import com.cyberrange.backend.event.service.SshPracticeScenario.Observation;
import com.cyberrange.backend.lab.repository.SshPracticeRepository;
import com.cyberrange.backend.lab.repository.SshPracticeRepository.History;

public record SshPracticeResponse(UUID id, long exerciseId, long assessmentId, long redTeamId, long blueTeamId, String role,
                       String state, boolean ready, boolean canStartScenario, String connectionStatus, Integer version, String target,
                       List<SshPracticeTaskResponse> criteria, List<Observation> observations, List<Observation> recovery,
                       boolean alert, JsonNode submission, Integer score, List<History> history,
                       List<SshPracticeRepository.ObservedEvent> observedEvents, SshPracticeMetricsResponse metrics) {}
