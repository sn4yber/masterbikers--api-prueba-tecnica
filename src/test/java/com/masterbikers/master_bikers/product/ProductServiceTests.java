package com.masterbikers.master_bikers.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masterbikers.master_bikers.common.exception.ConflictException;
import com.masterbikers.master_bikers.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

	@Mock
	private ProductRepository productRepository;

	private ProductService productService;

	@BeforeEach
	void setUp() {
		productService = new ProductService(productRepository, new ProductMapper());
	}

	@Test
	void createsManualProductWithDefaults() {
		ProductCreateRequest request = createRequest(null, null);
		when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService.create(request);

		assertNotNull(response.id());
		assertEquals(ProductAvailability.UNKNOWN, response.availability());
		assertEquals(ProductCondition.NEW, response.condition());
		assertEquals("Road Bike", response.name());
	}

	@Test
	void rejectsDuplicateExternalReference() {
		ProductCreateRequest request = createRequest("ext-1", ProductSource.AUTOMATION_EXERCISE);
		Product existing = Product.create(request);
		when(productRepository.findBySourceAndExternalId(ProductSource.AUTOMATION_EXERCISE, "ext-1"))
				.thenReturn(Optional.of(existing));

		assertThrows(ConflictException.class, () -> productService.create(request));
		verify(productRepository, never()).saveAndFlush(any(Product.class));
	}

	@Test
	void createsExternalProductWhenItDoesNotExist() {
		ProductCreateRequest request = createRequest("ext-2", ProductSource.AUTOMATION_EXERCISE);
		when(productRepository.findBySourceAndExternalId(ProductSource.AUTOMATION_EXERCISE, "ext-2"))
				.thenReturn(Optional.empty());
		when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService.upsertExternal(request);

		assertEquals("ext-2", response.externalId());
		assertEquals(ProductSource.AUTOMATION_EXERCISE, response.source());
	}

	@Test
	void synchronizesExistingExternalProduct() {
		Product product = Product.create(createRequest("ext-3", ProductSource.AUTOMATION_EXERCISE));
		ProductCreateRequest updated = new ProductCreateRequest(
				"Updated Bike", null, new BigDecimal("1500.00"), null, ProductAvailability.IN_STOCK,
				ProductCondition.USED, null, "https://example.test/3", "ext-3", ProductSource.AUTOMATION_EXERCISE);
		when(productRepository.findBySourceAndExternalId(ProductSource.AUTOMATION_EXERCISE, "ext-3"))
				.thenReturn(Optional.of(product));
		when(productRepository.saveAndFlush(product)).thenReturn(product);

		ProductResponse response = productService.upsertExternal(updated);

		assertEquals("Updated Bike", response.name());
		assertEquals(new BigDecimal("1500.00"), response.price());
		assertEquals(ProductCondition.USED, response.condition());
	}

	@Test
	void reportsMissingProduct() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.get(id));
	}

	@Test
	void patchesOnlyProvidedFields() {
		Product product = Product.create(createRequest(null, null));
		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
		when(productRepository.saveAndFlush(product)).thenReturn(product);
		ProductUpdateRequest request = new ProductUpdateRequest(
				"Updated Bike", null, new BigDecimal("1200.00"), null, null, null, null, null, null, null);

		ProductResponse response = productService.update(product.getId(), request);

		assertEquals("Updated Bike", response.name());
		assertEquals("Lightweight road bike", response.description());
		assertEquals(new BigDecimal("1200.00"), response.price());
	}

	@Test
	void deletesExistingProduct() {
		Product product = Product.create(createRequest(null, null));
		when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

		productService.delete(product.getId());

		verify(productRepository).delete(product);
	}

	private ProductCreateRequest createRequest(String externalId, ProductSource source) {
		return new ProductCreateRequest(
				"Road Bike",
				"Lightweight road bike",
				new BigDecimal("999.99"),
				"Bikes",
				null,
				null,
				"Master",
				null,
				externalId,
				source);
	}
}
