package com.cyberrange.backend.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.WINDOWS)
class WindowsProcessJobTest {
    @Test
    void closesTheChildAndItsDescendant(@TempDir Path temp) throws Exception {
        var log = temp.resolve("process.log");
        var java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        var classes = Path.of(WindowsProcessJobTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        long childPid;
        long descendantPid = 0;
        try (var job = new WindowsProcessJob()) {
            var child = job.start(List.of(java, "-cp", classes, Child.class.getName(), "value with spaces"), temp,
                    Map.of("SystemRoot", System.getenv("SystemRoot"), "CYBER_RANGE_TEST", "inherited"), log);
            childPid = child.pid();
            long deadline = System.nanoTime() + Duration.ofSeconds(12).toNanos();
            while (child.alive() && System.nanoTime() < deadline) {
                String output = Files.readString(log).strip();
                if (output.matches("[0-9]+")) {
                    descendantPid = Long.parseLong(output);
                    break;
                }
                Thread.sleep(100);
            }
            assertTrue(descendantPid > 0, () -> "The child did not report its descendant: " + log);
            assertTrue(alive(descendantPid));
        }
        assertFalse(alive(childPid));
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (alive(descendantPid) && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertFalse(alive(descendantPid));
    }

    private static boolean alive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    public static class Child {
        public static void main(String[] args) throws Exception {
            if (args.length == 0) {
                Thread.sleep(Duration.ofMinutes(1));
                return;
            }
            if (!"value with spaces".equals(args[0]) || !"inherited".equals(System.getenv("CYBER_RANGE_TEST"))) {
                throw new IllegalStateException("Child launch did not preserve the argument or environment");
            }
            var java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
            var descendant = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), Child.class.getName()).start();
            System.out.println(descendant.pid());
            descendant.waitFor();
        }
    }
}
