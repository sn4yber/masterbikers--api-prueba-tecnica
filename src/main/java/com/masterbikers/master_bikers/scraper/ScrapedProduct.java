package com.masterbikers.master_bikers.scraper;

import java.math.BigDecimal;

import com.masterbikers.master_bikers.product.ProductAvailability;
import com.masterbikers.master_bikers.product.ProductCondition;

public record ScrapedProduct(
		String externalId,
		String name,
		BigDecimal price,
		String category,
		ProductAvailability availability,
		ProductCondition condition,
		String brand,
		String sourceUrl) {
}
