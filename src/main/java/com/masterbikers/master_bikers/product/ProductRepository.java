package com.masterbikers.master_bikers.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	Optional<Product> findBySourceAndExternalId(ProductSource source, String externalId);

	boolean existsBySourceAndExternalId(ProductSource source, String externalId);
}
