package com.cyberrange.backend.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
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
            var properties = new HashMap<String, Object>();
            properties.put("cyber.range.runtime.active", true);
            properties.put("spring.jpa.hibernate.ddl-auto", "validate");
            properties.put("spring.sql.init.mode", "never");
            environment.getPropertySources().addLast(new MapPropertySource("managedCyberRangeDefaults",
                    Map.of("DATABASE_USERNAME", "postgres")));
            validateConfiguration(environment);
            properties.put("spring.datasource.url", JDBC);
            var random = new SecureRandom();
            properties.put(RUNNER_TOKEN, token(random));
            properties.put(COLLECTOR_TOKEN, token(random));
            environment.getPropertySources().addFirst(new MapPropertySource("managedCyberRange", properties));
            var tools = RuntimeTools.preflight();
            context.getBeanFactory().registerSingleton("cyberRangeRuntimeTools", tools);
        });
    }

    static void validateConfiguration(Environment environment) {
        var errors = new ArrayList<String>();
        if (!"127.0.0.1".equals(resolve(environment, "server.address"))) {
            errors.add("SERVER_ADDRESS must be 127.0.0.1.");
        }
        if (!"8080".equals(resolve(environment, "server.port"))) {
            errors.add("SERVER_PORT must be 8080.");
        }
        String database = resolve(environment, "spring.datasource.url");
        if (database == null || !database.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):5432/cyber_range_db(\\?currentSchema=public)?")) {
            errors.add("DATABASE_URL must use localhost:5432/cyber_range_db with schema public.");
        }
        if (resolve(environment, "spring.datasource.username") == null) {
            errors.add("DATABASE_USERNAME must not be blank (default: postgres).");
        }
        if (resolve(environment, "spring.datasource.password") == null) {
            errors.add("DATABASE_PASSWORD is missing.");
        }
        String jwt = resolve(environment, "app.jwt.secret");
        if (jwt == null) {
            errors.add("JWT_SECRET is missing.");
        } else if (!validJwt(jwt)) {
            errors.add("JWT_SECRET must be Base64 with at least 32 decoded bytes.");
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Managed Cyber Range configuration is invalid. Set credentials in the environment"
                    + " or %LOCALAPPDATA%\\CyberRange\\application.properties."
                    + System.lineSeparator() + "- " + String.join(System.lineSeparator() + "- ", errors));
        }
    }

    private static String resolve(Environment environment, String property) {
        try {
            String value = environment.getProperty(property);
            return value == null || value.isBlank() || value.startsWith("${") ? null : value;
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static boolean validJwt(String jwt) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(jwt);
        } catch (IllegalArgumentException error) {
            return false;
        }
        try {
            return key.length >= 32;
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
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
