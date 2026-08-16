package com.masterbikers.master_bikers.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI masterBikersOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Master Bikers API")
				.description("Product management and asynchronous HTML extraction service")
				.version("1.0.0"));
	}
}
