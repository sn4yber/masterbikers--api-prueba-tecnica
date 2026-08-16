package com.masterbikers.master_bikers.product;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.masterbikers.master_bikers.common.exception.ConflictException;
import com.masterbikers.master_bikers.common.exception.ResourceNotFoundException;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductMapper productMapper;

	public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
		this.productRepository = productRepository;
		this.productMapper = productMapper;
	}

	@Transactional
	public ProductResponse create(ProductCreateRequest request) {
		ensureExternalReferenceAvailable(request.source(), request.externalId(), null);
		try {
			return productMapper.toResponse(productRepository.saveAndFlush(Product.create(request)));
		}
		catch (DataIntegrityViolationException exception) {
			throw duplicateExternalReference(exception);
		}
	}

	@Transactional
	public ProductResponse upsertExternal(ProductCreateRequest request) {
		Objects.requireNonNull(request.source(), "source is required");
		Objects.requireNonNull(request.externalId(), "externalId is required");
		Product product = productRepository.findBySourceAndExternalId(request.source(), request.externalId())
				.map(existing -> {
					existing.synchronize(request);
					return existing;
				})
				.orElseGet(() -> Product.create(request));
		return productMapper.toResponse(productRepository.saveAndFlush(product));
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> list(ProductFilter filter, Pageable pageable) {
		Specification<Product> specification = (root, query, builder) -> builder.conjunction();
		specification = specification
				.and(containsIgnoreCase("name", filter.name()))
				.and(containsIgnoreCase("category", filter.category()))
				.and(equalsValue("availability", filter.availability()))
				.and(equalsValue("condition", filter.condition()))
				.and(containsIgnoreCase("brand", filter.brand()))
				.and(equalsValue("source", filter.source()));
		return productRepository.findAll(specification, pageable).map(productMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public ProductResponse get(UUID id) {
		return productMapper.toResponse(findProduct(id));
	}

	@Transactional
	public ProductResponse update(UUID id, ProductUpdateRequest request) {
		Product product = findProduct(id);
		if (request.source() != null || request.externalId() != null) {
			ensureExternalReferenceAvailable(request.source(), request.externalId(), id);
		}
		product.update(request);
		try {
			return productMapper.toResponse(productRepository.saveAndFlush(product));
		}
		catch (DataIntegrityViolationException exception) {
			throw duplicateExternalReference(exception);
		}
	}

	@Transactional
	public void delete(UUID id) {
		productRepository.delete(findProduct(id));
	}

	private Specification<Product> containsIgnoreCase(String field, String value) {
		if (value == null || value.isBlank()) {
			return Specification.unrestricted();
		}
		String escaped = value.strip().toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return (root, query, builder) -> builder.like(
				builder.lower(root.get(field)), "%" + escaped + "%", '\\');
	}

	private Specification<Product> equalsValue(String field, Object value) {
		return value == null
				? Specification.unrestricted()
				: (root, query, builder) -> builder.equal(root.get(field), value);
	}

	private Product findProduct(UUID id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
	}

	private void ensureExternalReferenceAvailable(ProductSource source, String externalId, UUID currentProductId) {
		if (source == null || externalId == null) {
			return;
		}
		productRepository.findBySourceAndExternalId(source, externalId)
				.filter(existing -> !existing.getId().equals(currentProductId))
				.ifPresent(existing -> {
					throw new ConflictException("A product with this external reference already exists");
				});
	}

	private ConflictException duplicateExternalReference(DataIntegrityViolationException cause) {
		return new ConflictException("A product with this external reference already exists", cause);
	}
}
