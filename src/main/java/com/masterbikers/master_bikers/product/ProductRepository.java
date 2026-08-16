package com.masterbikers.master_bikers.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

	Optional<Product> findBySourceAndExternalId(ProductSource source, String externalId);

	boolean existsBySourceAndExternalId(ProductSource source, String externalId);
}
