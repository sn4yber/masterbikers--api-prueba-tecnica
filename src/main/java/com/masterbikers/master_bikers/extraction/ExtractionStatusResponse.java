package com.masterbikers.master_bikers.extraction;

import java.time.Instant;
import java.util.UUID;

public record ExtractionStatusResponse(
		UUID id,
		ExtractionStatus status,
		int total,
		long processed,
		long successful,
		long failed,
		Instant createdAt,
		Instant startedAt,
		Instant finishedAt) {
}
