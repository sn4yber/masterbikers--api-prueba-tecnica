package com.masterbikers.master_bikers.product;

public record ProductFilter(
		String name,
		String category,
		ProductAvailability availability,
		ProductCondition condition,
		String brand,
		ProductSource source) {
}
