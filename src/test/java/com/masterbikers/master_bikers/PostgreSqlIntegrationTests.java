package com.masterbikers.master_bikers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.masterbikers.master_bikers.extraction.ExtractionCreateRequest;
import com.masterbikers.master_bikers.extraction.ExtractionCreationTransaction;
import com.masterbikers.master_bikers.extraction.ExtractionItemRepository;
import com.masterbikers.master_bikers.extraction.ExtractionJobRepository;
import com.masterbikers.master_bikers.extraction.ExtractionRequestHasher;
import com.masterbikers.master_bikers.extraction.ExtractionService;
import com.masterbikers.master_bikers.product.ProductAvailability;
import com.masterbikers.master_bikers.product.ProductCondition;
import com.masterbikers.master_bikers.product.ProductCreateRequest;
import com.masterbikers.master_bikers.product.ProductFilter;
import com.masterbikers.master_bikers.product.ProductMapper;
import com.masterbikers.master_bikers.product.ProductService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
		ProductService.class,
		ProductMapper.class,
		ExtractionService.class,
		ExtractionCreationTransaction.class,
		ExtractionRequestHasher.class
})
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:18-alpine"));

	@Autowired
	private ProductService productService;

	@Autowired
	private ExtractionService extractionService;

	@Autowired
	private ExtractionJobRepository jobRepository;

	@Autowired
	private ExtractionItemRepository itemRepository;

	@Test
	void appliesFlywayMigrationsAndFiltersPagedProducts() {
		productService.create(product("Road Bike", "Bikes", ProductAvailability.IN_STOCK, "Master"));
		productService.create(product("Urban Helmet", "Protection", ProductAvailability.OUT_OF_STOCK, "Safe"));
		ProductFilter filter = new ProductFilter(
				"road", "bike", ProductAvailability.IN_STOCK, ProductCondition.NEW, "master", null);

		var page = productService.list(filter, PageRequest.of(0, 1, Sort.by("name")));

		assertEquals(1, page.getTotalElements());
		assertEquals("Road Bike", page.getContent().getFirst().name());
	}

	@Test
	void reusesJobForEquivalentExtractionRequest() {
		var first = extractionService.create(new ExtractionCreateRequest(List.of(3L, 1L)));
		var equivalent = extractionService.create(new ExtractionCreateRequest(List.of(1L, 3L)));

		assertEquals(first.id(), equivalent.id());
		assertEquals(1, jobRepository.count());
		assertEquals(2, itemRepository.count());
	}

	private ProductCreateRequest product(
			String name, String category, ProductAvailability availability, String brand) {
		return new ProductCreateRequest(
				name,
				null,
				new BigDecimal("100.00"),
				category,
				availability,
				ProductCondition.NEW,
				brand,
				null,
				null,
				null);
	}
}
