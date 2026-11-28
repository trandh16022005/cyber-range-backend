package com.cyberrange.backend.event.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.event.service.SshObservationService;
import com.cyberrange.backend.event.service.SshObservationService.Observation;
import com.cyberrange.backend.event.service.SshObservationService.Receipt;
import com.cyberrange.backend.lab.service.SshRunnerCredential;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class SshObservationController {

    private final SshRunnerCredential credentials;
    private final SshObservationService observations;

    public SshObservationController(SshRunnerCredential credentials, SshObservationService observations) {
        this.credentials = credentials;
        this.observations = observations;
    }

    @GetMapping("/api/v1/lab-agent/{id}/collector-health")
    public ApiResponse<Void> health(@PathVariable UUID id,
                                    @RequestHeader(value = "X-Lab-Credential", required = false) String token) {
        credentials.requireCollector(token);
        observations.requireSession(id);
        return ApiResponse.success("Collector authenticated");
    }

    @PostMapping("/api/v1/lab-agent/{id}/ssh-events")
    public ApiResponse<List<Receipt>> ingest(@PathVariable UUID id,
                                            @RequestHeader(value = "X-Lab-Credential", required = false) String token,
                                            @RequestBody List<Observation> batch) {
        credentials.requireCollector(token);
        return ApiResponse.success("SSH events accepted", observations.ingest(id, batch));
    }
}
