package com.masterbikers.master_bikers.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
		UUID id,
		String externalId,
		ProductSource source,
		String name,
		String description,
		BigDecimal price,
		String category,
		ProductAvailability availability,
		ProductCondition condition,
		String brand,
		String sourceUrl,
		Instant createdAt,
		Instant updatedAt) {
}
