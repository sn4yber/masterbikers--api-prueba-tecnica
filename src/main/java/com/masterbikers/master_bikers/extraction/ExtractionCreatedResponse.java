package com.masterbikers.master_bikers.extraction;

import java.util.UUID;

public record ExtractionCreatedResponse(UUID id, ExtractionStatus status) {
}
