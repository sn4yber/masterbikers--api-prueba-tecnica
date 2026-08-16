package com.masterbikers.master_bikers.extraction;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.masterbikers.master_bikers.product.ProductCreateRequest;
import com.masterbikers.master_bikers.product.ProductResponse;
import com.masterbikers.master_bikers.product.ProductService;
import com.masterbikers.master_bikers.product.ProductSource;
import com.masterbikers.master_bikers.scraper.AutomationExerciseScraper;
import com.masterbikers.master_bikers.scraper.ScrapedProduct;
import com.masterbikers.master_bikers.scraper.ScrapingException;

@Component
public class ExtractionItemProcessor {

	private static final Logger log = LoggerFactory.getLogger(ExtractionItemProcessor.class);

	private final ExtractionStateService stateService;
	private final AutomationExerciseScraper scraper;
	private final ProductService productService;

	public ExtractionItemProcessor(
			ExtractionStateService stateService,
			AutomationExerciseScraper scraper,
			ProductService productService) {
		this.stateService = stateService;
		this.scraper = scraper;
		this.productService = productService;
	}

	public void process(UUID itemId) {
		try {
			Optional<String> externalProductId = stateService.startItem(itemId);
			if (externalProductId.isEmpty()) {
				return;
			}
			ScrapedProduct scraped = scraper.scrape(Long.parseLong(externalProductId.get()));
			ProductResponse product = productService.upsertExternal(toRequest(scraped));
			stateService.succeedItem(itemId, product.id());
		}
		catch (Exception exception) {
			log.error("Extraction item {} failed", itemId, exception);
			try {
				stateService.failItem(itemId, safeMessage(exception));
			}
			catch (Exception persistenceException) {
				log.error("Extraction item {} failure state could not be persisted", itemId, persistenceException);
			}
		}
	}

	private ProductCreateRequest toRequest(ScrapedProduct scraped) {
		return new ProductCreateRequest(
				scraped.name(),
				null,
				scraped.price(),
				scraped.category(),
				scraped.availability(),
				scraped.condition(),
				scraped.brand(),
				scraped.sourceUrl(),
				scraped.externalId(),
				ProductSource.AUTOMATION_EXERCISE);
	}

	private String safeMessage(Exception exception) {
		if (exception instanceof ScrapingException && exception.getMessage() != null) {
			return truncate(exception.getMessage());
		}
		return "Product extraction failed";
	}

	private String truncate(String message) {
		return message.length() <= 1000 ? message : message.substring(0, 1000);
	}
}
