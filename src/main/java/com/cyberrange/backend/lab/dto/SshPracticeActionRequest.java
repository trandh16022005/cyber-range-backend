package com.cyberrange.backend.lab.dto;

import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;

public record SshPracticeActionRequest(@NotBlank String action, String note, List<Long> observationIds, String account, String target,
                         Boolean recovered, Boolean analysisAccepted, Boolean reportAccepted,
                         String source, String impact, Map<String, String> reportSections) {}
