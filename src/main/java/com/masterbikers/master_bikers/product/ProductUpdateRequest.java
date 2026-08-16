package com.masterbikers.master_bikers.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
		@Size(max = 200) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
		@Size(max = 10000) String description,
		@DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
		@Size(max = 100) String category,
		ProductAvailability availability,
		ProductCondition condition,
		@Size(max = 100) String brand,
		@Size(max = 2048) String sourceUrl,
		@Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String externalId,
		ProductSource source) {

	@AssertTrue(message = "externalId and source must either both be provided or both be null")
	public boolean isExternalReferenceValid() {
		return (externalId == null) == (source == null);
	}
}
