package com.masterbikers.master_bikers.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.masterbikers.master_bikers.product.ProductAvailability;
import com.masterbikers.master_bikers.product.ProductCondition;

class ProductParserTests {

	private final ProductParser parser = new ProductParser();

	@Test
	void parsesProductInformation() {
		Document document = Jsoup.parse("""
				<div class="product-information">
				  <h2>Mountain Bike</h2>
				  <span><span>Rs. 1,250.50</span></span>
				  <p><b>category:</b> Bikes</p>
				  <p><b>AVAILABILITY:</b> In Stock</p>
				  <p><b>Condition:</b> Refurbished</p>
				  <p><b>Brand:</b> Master</p>
				</div>
				""");

		ScrapedProduct product = parser.parse(document, "42", "https://example.test/product_details/42");

		assertEquals("42", product.externalId());
		assertEquals("Mountain Bike", product.name());
		assertEquals(new BigDecimal("1250.50"), product.price());
		assertEquals("Bikes", product.category());
		assertEquals(ProductAvailability.IN_STOCK, product.availability());
		assertEquals(ProductCondition.REFURBISHED, product.condition());
		assertEquals("Master", product.brand());
	}

	@Test
	void usesUnknownAndNullForMissingOptionalFields() {
		Document document = Jsoup.parse("""
				<div class="product-information">
				  <h2>Road Bike</h2>
				  <span><span>Rs. 500</span></span>
				</div>
				""");

		ScrapedProduct product = parser.parse(document, "1", "https://example.test/product_details/1");

		assertNull(product.category());
		assertNull(product.brand());
		assertEquals(ProductAvailability.UNKNOWN, product.availability());
		assertEquals(ProductCondition.UNKNOWN, product.condition());
	}

	@Test
	void rejectsMissingProductRoot() {
		Document document = Jsoup.parse("<html><body></body></html>");

		assertThrows(ScrapingException.class, () -> parser.parse(document, "1", "https://example.test"));
	}

	@Test
	void rejectsMissingName() {
		Document document = Jsoup.parse("""
				<div class="product-information">
				  <span><span>Rs. 500</span></span>
				</div>
				""");

		assertThrows(ScrapingException.class, () -> parser.parse(document, "1", "https://example.test"));
	}

	@Test
	void rejectsInvalidOrNonPositivePrice() {
		Document invalid = Jsoup.parse("""
				<div class="product-information"><h2>Bike</h2><span><span>unknown</span></span></div>
				""");
		Document zero = Jsoup.parse("""
				<div class="product-information"><h2>Bike</h2><span><span>Rs. 0</span></span></div>
				""");

		assertThrows(ScrapingException.class, () -> parser.parse(invalid, "1", "https://example.test"));
		assertThrows(ScrapingException.class, () -> parser.parse(zero, "1", "https://example.test"));
	}
}
