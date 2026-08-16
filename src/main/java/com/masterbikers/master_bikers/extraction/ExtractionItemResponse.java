package com.masterbikers.master_bikers.extraction;

import java.time.Instant;
import java.util.UUID;

public record ExtractionItemResponse(
		String externalProductId,
		ExtractionItemStatus status,
		UUID productId,
		String errorMessage,
		Instant processedAt) {
}
