package com.dreamoutbox.ams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@SpringBootApplication
public class AmsApplication {

	public static void main(String[] args) {
		ensureDatasourceDirectoryExists();
		SpringApplication.run(AmsApplication.class, args);
	}

	/**
	 * Reads the datasource URL from application.properties and creates the parent
	 * directory if it does not already exist (e.g. /data for
	 * jdbc:sqlite:/data/ams.db).
	 */
	private static void ensureDatasourceDirectoryExists() {
		try (InputStream in = AmsApplication.class.getClassLoader()
				.getResourceAsStream("application.properties")) {
			if (in == null)
				return;
			Properties props = new Properties();
			props.load(in);
			String url = props.getProperty("spring.datasource.url", "");
			// Handle jdbc:sqlite:/path/to/file.db
			if (url.startsWith("jdbc:sqlite:")) {
				String filePath = url.substring("jdbc:sqlite:".length());
				Path dbFile = Paths.get(filePath);
				Path dir = dbFile.getParent();
				if (dir != null && !Files.exists(dir)) {
					Files.createDirectories(dir);
					System.out.println("Created data directory: " + dir);
				}
			}
		} catch (IOException e) {
			System.err.println("Warning: could not ensure datasource directory exists: " + e.getMessage());
		}
	}

}
