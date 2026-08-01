package com.dreamoutbox.ams.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseDirectoryInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String url = applicationContext.getEnvironment().getProperty("spring.datasource.url", "");
        if (url.startsWith("jdbc:sqlite:")) {
            String filePath = url.substring("jdbc:sqlite:".length());
            Path dbFile = Paths.get(filePath);
            Path dir = dbFile.getParent();
            if (dir != null && !Files.exists(dir)) {
                try {
                    Files.createDirectories(dir);
                    System.out.println("Created data directory: " + dir.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Warning: could not create database directory: " + e.getMessage());
                }
            }
        }
    }
}
