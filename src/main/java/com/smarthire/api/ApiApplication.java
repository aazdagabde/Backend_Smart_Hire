package com.smarthire.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// --- IMPORTS À AJOUTER ---
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
// --- FIN DES IMPORTS ---

// --- ANNOTATIONS À AJOUTER ---
@OpenAPIDefinition(
		info = @Info(title = "SmartHire API", version = "v1.0"),
		security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
		name = "Bearer Authentication",
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer"
)
// --- FIN DES ANNOTATIONS ---
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}