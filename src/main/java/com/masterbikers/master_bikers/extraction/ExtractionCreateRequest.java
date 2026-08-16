package com.masterbikers.master_bikers.extraction;

import java.util.HashSet;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExtractionCreateRequest(
		@NotEmpty @Size(max = 50) List<@NotNull @Positive Long> productIds) {

	@AssertTrue(message = "productIds must not contain duplicates")
	public boolean isWithoutDuplicates() {
		return productIds == null || new HashSet<>(productIds).size() == productIds.size();
	}
}
