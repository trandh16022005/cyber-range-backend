package com.cyberrange.backend.runtime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

record RuntimeTools(Path root, String python, String node, String docker, Map<String, String> childEnvironment) {
    private static final List<String> CHILD_VARIABLES = List.of("PATH", "PATHEXT", "SystemRoot", "WINDIR", "ComSpec",
            "TEMP", "TMP", "USERPROFILE", "APPDATA", "LOCALAPPDATA", "HOMEDRIVE", "HOMEPATH", "PROGRAMDATA",
            "ProgramFiles", "ProgramFiles(x86)", "PROCESSOR_ARCHITECTURE", "DOCKER_CONTEXT", "DOCKER_HOST", "DOCKER_CONFIG");

    static RuntimeTools preflight() {
        var root = Path.of("").toAbsolutePath().normalize();
        if (!Files.isRegularFile(root.resolve("cyber-range-lab/runner.py"))
                || !Files.isRegularFile(root.resolve("cyber-range-lab/compose.yaml"))) {
            throw new IllegalStateException("Use the project root as the working directory in IntelliJ or PowerShell.");
        }
        if (!Files.isRegularFile(root.resolve("frontend/node_modules/vite/bin/vite.js"))) {
            throw new IllegalStateException("Vite dependencies are missing. Run npm ci in the frontend directory.");
        }
        checkPort(ManagedRuntime.BACKEND_PORT);
        checkPort(ManagedRuntime.FRONTEND_PORT);
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 5432), 2000);
        } catch (IOException error) {
            throw new IllegalStateException("Start PostgreSQL on 127.0.0.1:5432 before running Cyber Range.", error);
        }
        var environment = new HashMap<String, String>();
        for (String name : CHILD_VARIABLES) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                environment.put(name, value);
            }
        }
        var tools = new RuntimeTools(root, executable("python.exe"), executable("node.exe"), executable("docker.exe"),
                Map.copyOf(environment));
        tools.run("Python", tools.python, "--version");
        tools.run("Node.js", tools.node, "--version");
        String endpoint = environment.get("DOCKER_HOST");
        if (environment.containsKey("DOCKER_CONTEXT") || endpoint == null) {
            String context = tools.run("Docker context", tools.docker, "context", "show");
            endpoint = tools.run("Docker context", tools.docker, "context", "inspect", context,
                    "--format", "{{.Endpoints.docker.Host}}");
        }
        if (!endpoint.matches("(?i)npipe:/+\\./pipe/[^\\s/]+")) {
            throw new IllegalStateException("Select a local Docker Desktop named-pipe context before running Cyber Range.");
        }
        if (!"linux".equals(tools.run("Docker Desktop", tools.docker, "info", "--format", "{{.OSType}}"))) {
            throw new IllegalStateException("Switch Docker Desktop to Linux containers.");
        }
        tools.run("Docker Compose configuration", tools.docker, "compose", "-f", "cyber-range-lab/compose.yaml", "config", "-q");
        return tools;
    }

    private static String executable(String name) {
        String searchPath = System.getenv("PATH");
        for (String entry : (searchPath == null ? "" : searchPath).split(";")) {
            if (!entry.isBlank()) {
                var path = Path.of(entry.replace("\"", ""));
                if (path.isAbsolute() && Files.isRegularFile(path.resolve(name))) {
                    return path.resolve(name).normalize().toString();
                }
            }
        }
        throw new IllegalStateException("Missing " + name + " on PATH. See the Windows startup section in README.md.");
    }

    private static void checkPort(int port) {
        try (var socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("127.0.0.1", port));
        } catch (IOException error) {
            throw new IllegalStateException("Port " + port + " is unavailable. Check Get-NetTCPConnection -LocalPort " + port + ".", error);
        }
    }

    private String run(String name, String... command) {
        Process process = null;
        try {
            var builder = new ProcessBuilder(command).directory(root.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.environment().clear();
            builder.environment().putAll(childEnvironment);
            process = builder.start();
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException(name + " preflight failed. Check the local installation or service, then retry.");
            }
            return new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).strip();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot run the " + name + " preflight.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(name + " preflight interrupted.", error);
        } finally {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }
}
