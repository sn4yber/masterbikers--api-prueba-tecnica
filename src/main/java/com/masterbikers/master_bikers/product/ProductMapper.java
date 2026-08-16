package com.masterbikers.master_bikers.product;

import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

	public ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getExternalId(),
				product.getSource(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getCategory(),
				product.getAvailability(),
				product.getCondition(),
				product.getBrand(),
				product.getSourceUrl(),
				product.getCreatedAt(),
				product.getUpdatedAt());
	}
}
