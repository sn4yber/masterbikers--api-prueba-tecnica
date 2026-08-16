package com.masterbikers.master_bikers.scraper;

import java.math.BigDecimal;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.masterbikers.master_bikers.product.ProductAvailability;
import com.masterbikers.master_bikers.product.ProductCondition;

@Component
public class ProductParser {

	public ScrapedProduct parse(Document document, String externalId, String sourceUrl) {
		Element root = document == null ? null : document.selectFirst(".product-information");
		if (root == null) {
			throw new ScrapingException("Product information is unavailable");
		}
		Element nameElement = root.selectFirst("h2");
		if (nameElement == null || nameElement.text().isBlank()) {
			throw new ScrapingException("Product name is unavailable");
		}
		Element priceElement = root.selectFirst("span span");
		BigDecimal price = parsePrice(priceElement == null ? null : priceElement.text());
		String category = field(root, "Category");
		String availability = field(root, "Availability");
		String condition = field(root, "Condition");
		String brand = field(root, "Brand");
		return new ScrapedProduct(
				externalId,
				nameElement.text().trim(),
				price,
				category,
				parseAvailability(availability),
				parseCondition(condition),
				brand,
				sourceUrl);
	}

	private BigDecimal parsePrice(String value) {
		if (value == null) {
			throw new ScrapingException("Product price is unavailable");
		}
		String normalized = value.trim().replaceFirst("(?i)^Rs\\.?\\s*", "").replace(",", "");
		try {
			BigDecimal price = new BigDecimal(normalized);
			if (price.signum() <= 0) {
				throw new ScrapingException("Product price must be positive");
			}
			return price;
		}
		catch (NumberFormatException exception) {
			throw new ScrapingException("Product price is invalid", exception);
		}
	}

	private String field(Element root, String label) {
		String prefix = label.toLowerCase(Locale.ROOT) + ":";
		for (Element paragraph : root.select("p")) {
			String text = paragraph.text().trim();
			if (text.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				String value = text.substring(prefix.length()).trim();
				return value.isEmpty() ? null : value;
			}
		}
		return null;
	}

	private ProductAvailability parseAvailability(String value) {
		if (value == null) {
			return ProductAvailability.UNKNOWN;
		}
		return switch (normalize(value)) {
			case "in stock" -> ProductAvailability.IN_STOCK;
			case "out of stock" -> ProductAvailability.OUT_OF_STOCK;
			default -> ProductAvailability.UNKNOWN;
		};
	}

	private ProductCondition parseCondition(String value) {
		if (value == null) {
			return ProductCondition.UNKNOWN;
		}
		return switch (normalize(value)) {
			case "new" -> ProductCondition.NEW;
			case "used" -> ProductCondition.USED;
			case "refurbished" -> ProductCondition.REFURBISHED;
			default -> ProductCondition.UNKNOWN;
		};
	}

	private String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}
}
