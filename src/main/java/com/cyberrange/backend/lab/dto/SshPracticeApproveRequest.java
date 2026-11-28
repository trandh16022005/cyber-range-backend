package com.cyberrange.backend.lab.dto;

import java.util.UUID;

public record SshPracticeApproveRequest(long redTeamId, UUID labSessionId) {}
