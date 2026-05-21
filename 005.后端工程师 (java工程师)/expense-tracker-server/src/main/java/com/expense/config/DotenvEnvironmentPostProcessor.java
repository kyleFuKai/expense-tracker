package com.expense.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads .env file from project root into Spring Environment.
 * Supports KEY=VALUE format with optional quoted values.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String ENV_PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication springApplication) {
        Path envFile = findEnvFile();
        if (envFile == null || !Files.exists(envFile)) {
            return;
        }

        Properties props = new Properties();
        try (InputStream is = new FileInputStream(envFile.toFile())) {
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load .env file: " + envFile, e);
        }

        Map<String, Object> map = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            // Skip if already set by OS environment or JVM args
            if (environment.containsProperty(name)) {
                continue;
            }
            map.put(name, props.getProperty(name));
        }

        if (!map.isEmpty()) {
            PropertySource<?> ps = new MapPropertySource(ENV_PROPERTY_SOURCE_NAME, map);
            MutablePropertySources sources = environment.getPropertySources();
            // Add with lowest priority so OS env vars and CLI args take precedence
            sources.addLast(ps);
        }
    }

    private Path findEnvFile() {
        // Check current working directory first (for mvn spring-boot:run)
        Path cwd = Path.of(".env").toAbsolutePath().normalize();
        if (Files.exists(cwd)) {
            return cwd;
        }
        // Check user home
        Path home = Path.of(System.getProperty("user.home"), ".env");
        if (Files.exists(home)) {
            return home;
        }
        return null;
    }
}
