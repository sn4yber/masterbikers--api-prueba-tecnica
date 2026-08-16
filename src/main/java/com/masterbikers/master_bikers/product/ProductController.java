package com.masterbikers.master_bikers.product;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
		ProductResponse product = productService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(product.id())
				.toUri();
		return ResponseEntity.created(location).body(product);
	}

	@GetMapping
	public List<ProductResponse> list() {
		return productService.list();
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable UUID id) {
		return productService.get(id);
	}

	@PatchMapping("/{id}")
	public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
