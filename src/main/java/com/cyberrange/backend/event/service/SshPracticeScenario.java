package com.cyberrange.backend.event.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class SshPracticeScenario {

    public record Observation(long id, int second, String source, String account, String target, String outcome) {}

    public record Criterion(String name, int maximum) {}

    private static final String TARGET = "ubuntu-target";
    private static final int THRESHOLD = 5;
    private static final Duration WINDOW = Duration.ofSeconds(60);
    public static final int VERSION = 1;
    public static final List<Observation> OBSERVATIONS = List.of(
            new Observation(1, 0, "192.0.2.42", "lab-user", TARGET, "FAILURE"),
            new Observation(2, 8, "192.0.2.42", "lab-user", TARGET, "FAILURE"),
            new Observation(3, 10, "192.0.2.77", "lab-user", TARGET, "SUCCESS"),
            new Observation(4, 14, "192.0.2.42", "lab-user", TARGET, "FAILURE"),
            new Observation(5, 20, "192.0.2.42", "lab-user", TARGET, "FAILURE"),
            new Observation(6, 30, "192.0.2.42", "lab-user", TARGET, "FAILURE"));
    public static final List<Observation> RECOVERY = List.of(
            new Observation(7, 90, "192.0.2.77", "lab-user", TARGET, "SUCCESS"));
    public static final List<Criterion> CRITERIA = List.of(
            new Criterion("Nhận diện lỗi xác thực", 10),
            new Criterion("Xác định nhóm đăng nhập bất thường", 15),
            new Criterion("Phân tích log và lập luận", 15),
            new Criterion("Xác định tài khoản và máy chủ", 15),
            new Criterion("Đề xuất biện pháp cô lập phù hợp", 20),
            new Criterion("Xác minh phục hồi", 15),
            new Criterion("Nộp báo cáo sự cố", 10));

    private SshPracticeScenario() {}

    public static Set<Long> alertObservations() {
        var failures = OBSERVATIONS.stream()
                .filter(row -> "FAILURE".equals(row.outcome()))
                .map(row -> new SshDetectionWindow.Failure(row.id(), Instant.EPOCH.plusSeconds(row.second())))
                .toList();
        return SshDetectionWindow.matching(failures, failures.getLast(), THRESHOLD, WINDOW)
                .orElseThrow().stream().map(SshDetectionWindow.Failure::id).collect(Collectors.toUnmodifiableSet());
    }
}
