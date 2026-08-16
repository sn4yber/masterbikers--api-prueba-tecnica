package com.masterbikers.master_bikers.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AutomationExerciseScraperTests {

	private static final String PRODUCT_HTML = """
			<div class="product-information">
			  <h2>Retry Bike</h2>
			  <span><span>Rs. 750</span></span>
			</div>
			""";

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void retriesTemporaryHttpFailureAndRecordsSuccessMetrics() throws IOException {
		AtomicInteger requests = new AtomicInteger();
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/product_details/7", exchange -> {
			int status = requests.incrementAndGet() == 1 ? 503 : 200;
			byte[] body = (status == 200 ? PRODUCT_HTML : "temporary").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		AutomationExerciseScraper scraper = scraper(registry, 3);

		ScrapedProduct product = scraper.scrape(7);

		assertEquals("Retry Bike", product.name());
		assertEquals(2, requests.get());
		assertEquals(1.0, registry.get("scraping.success").counter().count());
		assertEquals(0.0, registry.get("scraping.failure").counter().count());
		assertEquals(1L, registry.get("scraping.duration").timer().count());
	}

	@Test
	void doesNotRetryPermanentHttpFailureAndRecordsFailureMetrics() throws IOException {
		AtomicInteger requests = new AtomicInteger();
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/product_details/7", exchange -> {
			requests.incrementAndGet();
			byte[] body = "missing".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(404, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		AutomationExerciseScraper scraper = scraper(registry, 3);

		assertThrows(ScrapingException.class, () -> scraper.scrape(7));

		assertEquals(1, requests.get());
		assertEquals(0.0, registry.get("scraping.success").counter().count());
		assertEquals(1.0, registry.get("scraping.failure").counter().count());
		assertEquals(1L, registry.get("scraping.duration").timer().count());
	}

	private AutomationExerciseScraper scraper(SimpleMeterRegistry registry, int maxAttempts) {
		String baseUrl = "http://localhost:" + server.getAddress().getPort();
		return new AutomationExerciseScraper(
				new ProductParser(), registry, baseUrl, 1_000, maxAttempts, 1, 2);
	}
}
