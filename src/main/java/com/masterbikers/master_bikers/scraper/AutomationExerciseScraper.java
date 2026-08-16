package com.masterbikers.master_bikers.scraper;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AutomationExerciseScraper {

	private static final int MAX_TIMEOUT_MILLIS = 10_000;
	private static final int MAX_BODY_SIZE_BYTES = 1_048_576;
	private static final String USER_AGENT = "MasterBikersProductExtractor/1.0";

	private final ProductParser productParser;
	private final String baseUrl;
	private final int timeoutMillis;

	public AutomationExerciseScraper(
			ProductParser productParser,
			@Value("${scraper.automation-exercise.base-url}") String baseUrl,
			@Value("${scraper.automation-exercise.timeout-ms}") int timeoutMillis) {
		this.productParser = productParser;
		this.baseUrl = baseUrl.replaceAll("/+$", "");
		this.timeoutMillis = Math.min(Math.max(timeoutMillis, 1), MAX_TIMEOUT_MILLIS);
	}

	public ScrapedProduct scrape(long productId) {
		if (productId <= 0) {
			throw new IllegalArgumentException("productId must be positive");
		}
		String sourceUrl = baseUrl + "/product_details/" + productId;
		try {
			Document document = Jsoup.connect(sourceUrl)
					.userAgent(USER_AGENT)
					.timeout(timeoutMillis)
					.maxBodySize(MAX_BODY_SIZE_BYTES)
					.get();
			return productParser.parse(document, Long.toString(productId), sourceUrl);
		}
		catch (IOException exception) {
			throw new ScrapingException("Product page could not be retrieved", exception);
		}
	}
}
