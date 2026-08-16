package com.masterbikers.master_bikers.scraper;

import java.io.IOException;
import java.time.Duration;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class AutomationExerciseScraper {

	private static final int MAX_TIMEOUT_MILLIS = 10_000;
	private static final int MAX_BODY_SIZE_BYTES = 1_048_576;
	private static final int MAX_ATTEMPTS = 5;
	private static final long MAX_BACKOFF_MILLIS = 10_000;
	private static final String USER_AGENT = "MasterBikersProductExtractor/1.0";

	private final ProductParser productParser;
	private final MeterRegistry meterRegistry;
	private final Counter successCounter;
	private final Counter failureCounter;
	private final Timer durationTimer;
	private final String baseUrl;
	private final int timeoutMillis;
	private final int maxAttempts;
	private final long initialBackoffMillis;
	private final long maxBackoffMillis;

	public AutomationExerciseScraper(
			ProductParser productParser,
			MeterRegistry meterRegistry,
			@Value("${scraper.automation-exercise.base-url}") String baseUrl,
			@Value("${scraper.automation-exercise.timeout-ms}") int timeoutMillis,
			@Value("${scraper.automation-exercise.retry.max-attempts:3}") int maxAttempts,
			@Value("${scraper.automation-exercise.retry.initial-backoff-ms:200}") long initialBackoffMillis,
			@Value("${scraper.automation-exercise.retry.max-backoff-ms:2000}") long maxBackoffMillis) {
		this.productParser = productParser;
		this.meterRegistry = meterRegistry;
		this.baseUrl = baseUrl.replaceAll("/+$", "");
		this.timeoutMillis = Math.min(Math.max(timeoutMillis, 1), MAX_TIMEOUT_MILLIS);
		this.maxAttempts = Math.min(Math.max(maxAttempts, 1), MAX_ATTEMPTS);
		this.initialBackoffMillis = Math.min(Math.max(initialBackoffMillis, 0), MAX_BACKOFF_MILLIS);
		this.maxBackoffMillis = Math.min(
				Math.max(maxBackoffMillis, this.initialBackoffMillis), MAX_BACKOFF_MILLIS);
		this.successCounter = Counter.builder("scraping.success")
				.description("Successful product scraping operations")
				.register(meterRegistry);
		this.failureCounter = Counter.builder("scraping.failure")
				.description("Failed product scraping operations")
				.register(meterRegistry);
		this.durationTimer = Timer.builder("scraping.duration")
				.description("Product scraping operation duration")
				.register(meterRegistry);
	}

	public ScrapedProduct scrape(long productId) {
		if (productId <= 0) {
			throw new IllegalArgumentException("productId must be positive");
		}
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			ScrapedProduct product = scrapeWithRetry(productId);
			successCounter.increment();
			return product;
		}
		catch (RuntimeException exception) {
			failureCounter.increment();
			throw exception;
		}
		finally {
			sample.stop(durationTimer);
		}
	}

	private ScrapedProduct scrapeWithRetry(long productId) {
		String sourceUrl = baseUrl + "/product_details/" + productId;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				Document document = Jsoup.connect(sourceUrl)
						.userAgent(USER_AGENT)
						.timeout(timeoutMillis)
						.maxBodySize(MAX_BODY_SIZE_BYTES)
						.get();
				return productParser.parse(document, Long.toString(productId), sourceUrl);
			}
			catch (IOException exception) {
				if (attempt == maxAttempts || !isTemporary(exception)) {
					throw new ScrapingException("Product page could not be retrieved", exception);
				}
				waitBeforeRetry(attempt);
			}
		}
		throw new IllegalStateException("Retry loop completed without result");
	}

	private boolean isTemporary(IOException exception) {
		if (exception instanceof HttpStatusException statusException) {
			int status = statusException.getStatusCode();
			return status == 429 || status >= 500;
		}
		return true;
	}

	private void waitBeforeRetry(int failedAttempt) {
		long multiplier = 1L << Math.min(failedAttempt - 1, 30);
		long delayMillis = Math.min(initialBackoffMillis * multiplier, maxBackoffMillis);
		try {
			Thread.sleep(Duration.ofMillis(delayMillis));
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ScrapingException("Product page retrieval was interrupted", exception);
		}
	}
}
