package com.masterbikers.master_bikers.product;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
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
	public List<ProductResponse> list() {
		return productRepository.findAll().stream()
				.map(productMapper::toResponse)
				.toList();
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
