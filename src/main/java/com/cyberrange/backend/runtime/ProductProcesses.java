package com.cyberrange.backend.runtime;

import com.cyberrange.backend.lab.service.SshLabService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "cyber.range.runtime.active", havingValue = "true")
final class ProductProcesses implements ApplicationRunner, ApplicationListener<ContextClosedEvent>, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(ProductProcesses.class);
    private static final String RUNNER_READY = "Runner authenticated with Backend";
    private final RuntimeTools tools;
    private final Environment environment;
    private final SshLabService labs;
    private final ConfigurableApplicationContext context;
    private WindowsProcessJob job;
    private WindowsProcessJob.Child runner;
    private WindowsProcessJob.Child frontend;
    private ScheduledExecutorService monitor;
    private Path runnerLog;
    private Path frontendLog;
    private boolean runnerReady;
    private boolean frontendReady;
    private boolean started;

    ProductProcesses(RuntimeTools tools, Environment environment, SshLabService labs, ConfigurableApplicationContext context) {
        this.tools = tools;
        this.environment = environment;
        this.labs = labs;
        this.context = context;
        labs.allowStarts(false);
    }

    @Override
    public synchronized void run(ApplicationArguments arguments) throws IOException, InterruptedException {
        var logRoot = Files.createDirectories(tools.root().resolve("target/runtime-logs"));
        var logs = Files.createTempDirectory(logRoot, "startup-");
        runnerLog = logs.resolve("runner.log");
        frontendLog = logs.resolve("frontend.log");
        job = new WindowsProcessJob();
        try {
            var runnerEnvironment = new HashMap<>(tools.childEnvironment());
            runnerEnvironment.put(ManagedRuntime.RUNNER_TOKEN, environment.getRequiredProperty(ManagedRuntime.RUNNER_TOKEN));
            runnerEnvironment.put(ManagedRuntime.COLLECTOR_TOKEN, environment.getRequiredProperty(ManagedRuntime.COLLECTOR_TOKEN));
            runnerEnvironment.put("SSH_LAB_BACKEND_PORT", Integer.toString(ManagedRuntime.BACKEND_PORT));
            boolean practiceEnabled = environment.getProperty("ssh.practice.enabled", Boolean.class, false);
            runnerEnvironment.put("SSH_PRACTICE_ENABLED", Boolean.toString(practiceEnabled));
            runnerEnvironment.put("CYBER_RANGE_MANAGED_RUNNER", "true");
            runnerEnvironment.put("PYTHONIOENCODING", "utf-8");
            runner = job.start(List.of(tools.python(), "-B", "-u", "cyber-range-lab/runner.py"), tools.root(),
                    runnerEnvironment, runnerLog);
            awaitRunner();
            runnerReady = true;
            LOG.info("Runner authenticated with Backend on {} (PID {}, log {})", ManagedRuntime.BACKEND, runner.pid(), runnerLog);

            var viteEnvironment = new HashMap<>(tools.childEnvironment());
            viteEnvironment.keySet().removeIf(name -> name.startsWith("DOCKER_"));
            viteEnvironment.put("VITE_BACKEND_PROXY_TARGET", ManagedRuntime.BACKEND);
            frontend = job.start(List.of(tools.node(), "node_modules/vite/bin/vite.js", "--host", "127.0.0.1",
                            "--port", Integer.toString(ManagedRuntime.FRONTEND_PORT), "--strictPort"),
                    tools.root().resolve("frontend"), viteEnvironment, frontendLog);
            awaitFrontend();
            requireAlive("Runner", runner, runnerLog);
            frontendReady = true;
            started = true;
            labs.allowStarts(true);
            monitor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("cyber-range-process-monitor").factory());
            monitor.scheduleWithFixedDelay(this::checkProcesses, 2, 2, TimeUnit.SECONDS);
            LOG.info("Frontend ready at {} (PID {}, log {})", ManagedRuntime.FRONTEND, frontend.pid(), frontendLog);
        } catch (IOException | InterruptedException | RuntimeException error) {
            destroy();
            throw error;
        }
    }

    private void awaitRunner() throws IOException, InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        do {
            requireAlive("Runner", runner, runnerLog);
            try (var reader = Files.newBufferedReader(runnerLog)) {
                if (RUNNER_READY.equals(reader.readLine())) {
                    return;
                }
            }
            Thread.sleep(200);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Runner did not authenticate with Backend. Review " + runnerLog);
    }

    private void awaitFrontend() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        do {
            requireAlive("Frontend", frontend, frontendLog);
            if (frontendResponds()) {
                return;
            }
            Thread.sleep(200);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Frontend did not respond on " + ManagedRuntime.FRONTEND + ". Review " + frontendLog);
    }

    private boolean frontendResponds() {
        HttpURLConnection request = null;
        try {
            request = (HttpURLConnection) URI.create(ManagedRuntime.FRONTEND).toURL().openConnection(Proxy.NO_PROXY);
            request.setConnectTimeout(500);
            request.setReadTimeout(1500);
            request.setInstanceFollowRedirects(false);
            return request.getResponseCode() == 200;
        } catch (IOException error) {
            return false;
        } finally {
            if (request != null) {
                request.disconnect();
            }
        }
    }

    private static void requireAlive(String name, WindowsProcessJob.Child child, Path log) {
        if (!child.alive()) {
            throw new IllegalStateException(name + " exited with code " + child.exitCode() + ". Review " + log);
        }
    }

    private synchronized void checkProcesses() {
        if (runnerReady && !runner.alive()) {
            runnerReady = false;
            labs.allowStarts(false);
            LOG.error("Runner exited unexpectedly (code {}). Restart Cyber Range after reviewing {}", runner.exitCode(), runnerLog);
        }
        if (frontendReady && !frontend.alive()) {
            frontendReady = false;
            LOG.error("Frontend exited unexpectedly (code {}). Restart Cyber Range after reviewing {}", frontend.exitCode(), frontendLog);
        }
    }

    @Override
    public synchronized void onApplicationEvent(ContextClosedEvent event) {
        if (event.getApplicationContext() != context || job == null) {
            return;
        }
        labs.allowStarts(false);
        try {
            if (started) {
                stopLab();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for the Lab. The next startup can reconcile its state.");
        } catch (RuntimeException error) {
            LOG.error("Cannot complete the managed Lab stop. Review its state before opening another session.", error);
        } finally {
            destroy();
        }
    }

    private void stopLab() throws InterruptedException {
        // ContextClosedEvent is published before Spring stops the HTTP server and the database pool.
        var session = labs.requestManagedStop();
        if (session == null) {
            return;
        }
        LOG.info("Waiting for Runner to stop Lab {}", session);
        long deadline = System.nanoTime() + Duration.ofSeconds(50).toNanos();
        while (runner.alive() && System.nanoTime() < deadline) {
            if (labs.isStopped(session)) {
                LOG.info("Runner confirmed STOPPED for Lab {}", session);
                return;
            }
            Thread.sleep(500);
        }
        if (!labs.isStopped(session)) {
            LOG.warn("Lab {} is still pending STOPPED. The next Runner will retry the saved stop request.", session);
        }
    }

    @Override
    public synchronized void destroy() {
        if (monitor != null) {
            monitor.shutdownNow();
            monitor = null;
        }
        if (job != null) {
            runnerReady = false;
            frontendReady = false;
            job.close();
            job = null;
            LOG.info("Managed Runner and Frontend process tree closed.");
        }
    }
}
