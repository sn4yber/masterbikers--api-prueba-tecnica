package com.masterbikers.master_bikers.common.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	private static final int SCRAPER_MAX_CONCURRENCY = 3;

	@Bean(name = "extractionJobExecutor")
	Executor extractionJobExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("extraction-job-");
		return executor;
	}

	@Bean(name = "scraperExecutor")
	Executor scraperExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(SCRAPER_MAX_CONCURRENCY);
		executor.setMaxPoolSize(SCRAPER_MAX_CONCURRENCY);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("scraper-");
		return executor;
	}
}
