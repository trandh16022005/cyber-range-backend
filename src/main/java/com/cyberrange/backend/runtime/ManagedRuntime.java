package com.cyberrange.backend.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ManagedRuntime {
    static final String RUNNER_TOKEN = "SSH_LAB_RUNNER_TOKEN";
    static final String COLLECTOR_TOKEN = "SSH_LAB_COLLECTOR_TOKEN";
    static final int BACKEND_PORT = 8080;
    static final int FRONTEND_PORT = 5173;
    static final String BACKEND = "http://127.0.0.1:" + BACKEND_PORT;
    static final String FRONTEND = "http://127.0.0.1:" + FRONTEND_PORT;
    private static final String JDBC = "jdbc:postgresql://127.0.0.1:5432/cyber_range_db?currentSchema=public";

    private ManagedRuntime() {}

    public static void configure(SpringApplication application) {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows")
                || "false".equalsIgnoreCase(System.getenv("CYBER_RANGE_MANAGED"))) {
            return;
        }
        // DevTools would otherwise close the running Lab on an automatic classpath restart.
        System.setProperty("spring.devtools.restart.enabled", "false");
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            var path = Path.of(localAppData, "CyberRange", "application.properties");
            application.setDefaultProperties(Map.of("spring.config.additional-location", "optional:" + path.toUri()));
        }
        application.addInitializers(context -> {
            var environment = context.getEnvironment();
            String address = environment.getRequiredProperty("server.address");
            String port = environment.getRequiredProperty("server.port");
            String database = environment.getRequiredProperty("spring.datasource.url");
            if (!"127.0.0.1".equals(address) || !"8080".equals(port)) {
                throw new IllegalStateException("Managed Cyber Range requires server.address=127.0.0.1 and server.port=8080.");
            }
            if (!database.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):5432/cyber_range_db(\\?currentSchema=public)?")) {
                throw new IllegalStateException("Managed Cyber Range requires localhost:5432/cyber_range_db with schema public.");
            }
            var properties = new HashMap<String, Object>();
            properties.put("cyber.range.runtime.active", true);
            properties.put("spring.datasource.url", JDBC);
            properties.put("spring.jpa.hibernate.ddl-auto", "validate");
            properties.put("spring.sql.init.mode", "never");
            properties.put("DATABASE_USERNAME", environment.getProperty("DATABASE_USERNAME", "postgres"));
            var random = new SecureRandom();
            properties.put(RUNNER_TOKEN, token(random));
            properties.put(COLLECTOR_TOKEN, token(random));
            environment.getPropertySources().addFirst(new MapPropertySource("managedCyberRange", properties));
            requireSecret(environment, "spring.datasource.password", "DATABASE_PASSWORD");
            String jwt = requireSecret(environment, "app.jwt.secret", "JWT_SECRET");
            try {
                var key = Base64.getDecoder().decode(jwt);
                try {
                    if (key.length < 32) {
                        throw new IllegalArgumentException();
                    }
                } finally {
                    java.util.Arrays.fill(key, (byte) 0);
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException("JWT_SECRET must be Base64 with at least 32 decoded bytes.");
            }
            var tools = RuntimeTools.preflight();
            context.getBeanFactory().registerSingleton("cyberRangeRuntimeTools", tools);
        });
    }

    private static String requireSecret(Environment environment, String property, String name) {
        String value;
        try {
            value = environment.getProperty(property);
        } catch (IllegalArgumentException error) {
            value = null;
        }
        if (value == null || value.isBlank() || value.equals("${" + name + "}")) {
            throw new IllegalStateException("Set " + name + " in the environment or %LOCALAPPDATA%\\CyberRange\\application.properties.");
        }
        return value;
    }

    private static String token(SecureRandom random) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } finally {
            java.util.Arrays.fill(bytes, (byte) 0);
        }
    }
}
