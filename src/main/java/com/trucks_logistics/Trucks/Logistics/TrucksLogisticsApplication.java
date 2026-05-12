package com.trucks_logistics.Trucks.Logistics;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class TrucksLogisticsApplication {

	public static void main(String[] args) {
		// Cargar variables de .env y setearlas como propiedades por defecto
		Dotenv dotenv = Dotenv.load();
		Map<String, Object> defaultProperties = new HashMap<>();
		// Db properties
		defaultProperties.put("DB_URL", dotenv.get("DB_URL"));
		defaultProperties.put("DB_USERNAME", dotenv.get("DB_USERNAME"));
		defaultProperties.put("DB_PASSWORD", dotenv.get("DB_PASSWORD"));

		// Mailtrap properties
		defaultProperties.put("MAILTRAP_HOST", dotenv.get("MAILTRAP_HOST"));
		defaultProperties.put("MAILTRAP_PORT", dotenv.get("MAILTRAP_PORT"));
		defaultProperties.put("MAILTRAP_USERNAME", dotenv.get("MAILTRAP_USERNAME"));
		defaultProperties.put("MAILTRAP_PASSWORD", dotenv.get("MAILTRAP_PASSWORD"));

		// Jwt secret
		defaultProperties.put("JWT_SECRET", dotenv.get("SECRET_KEY"));

		SpringApplication app = new SpringApplication(TrucksLogisticsApplication.class);
		app.setDefaultProperties(defaultProperties);
		app.run(args);
	}
}
