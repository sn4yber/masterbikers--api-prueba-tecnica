package com.masterbikers.master_bikers.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "external_id", length = 255)
	private String externalId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", length = 50)
	private ProductSource source;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "category", length = 100)
	private String category;

	@Enumerated(EnumType.STRING)
	@Column(name = "availability", nullable = false, length = 20)
	private ProductAvailability availability;

	@Enumerated(EnumType.STRING)
	@Column(name = "condition", nullable = false, length = 20)
	private ProductCondition condition;

	@Column(name = "brand", length = 100)
	private String brand;

	@Column(name = "source_url", length = 2048)
	private String sourceUrl;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Product() {
	}

	private Product(ProductCreateRequest request) {
		this.id = UUID.randomUUID();
		this.externalId = request.externalId();
		this.source = request.source();
		this.name = request.name();
		this.description = request.description();
		this.price = request.price();
		this.category = request.category();
		this.availability = request.availability() == null ? ProductAvailability.UNKNOWN : request.availability();
		this.condition = request.condition() == null ? ProductCondition.NEW : request.condition();
		this.brand = request.brand();
		this.sourceUrl = request.sourceUrl();
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static Product create(ProductCreateRequest request) {
		return new Product(request);
	}

	public void synchronize(ProductCreateRequest request) {
		this.externalId = request.externalId();
		this.source = request.source();
		this.name = request.name();
		this.description = request.description();
		this.price = request.price();
		this.category = request.category();
		this.availability = request.availability() == null ? ProductAvailability.UNKNOWN : request.availability();
		this.condition = request.condition() == null ? ProductCondition.UNKNOWN : request.condition();
		this.brand = request.brand();
		this.sourceUrl = request.sourceUrl();
		this.updatedAt = Instant.now();
	}

	public void update(ProductUpdateRequest request) {
		if (request.name() != null) {
			this.name = request.name();
		}
		if (request.description() != null) {
			this.description = request.description();
		}
		if (request.price() != null) {
			this.price = request.price();
		}
		if (request.category() != null) {
			this.category = request.category();
		}
		if (request.availability() != null) {
			this.availability = request.availability();
		}
		if (request.condition() != null) {
			this.condition = request.condition();
		}
		if (request.brand() != null) {
			this.brand = request.brand();
		}
		if (request.sourceUrl() != null) {
			this.sourceUrl = request.sourceUrl();
		}
		if ((request.externalId() == null) != (request.source() == null)) {
			throw new IllegalArgumentException("externalId and source must both be provided");
		}
		if (request.externalId() != null) {
			this.externalId = request.externalId();
			this.source = request.source();
		}
		this.updatedAt = Instant.now();
	}

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (availability == null) {
			availability = ProductAvailability.UNKNOWN;
		}
		if (condition == null) {
			condition = ProductCondition.NEW;
		}
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getExternalId() {
		return externalId;
	}

	public ProductSource getSource() {
		return source;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String getCategory() {
		return category;
	}

	public ProductAvailability getAvailability() {
		return availability;
	}

	public ProductCondition getCondition() {
		return condition;
	}

	public String getBrand() {
		return brand;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
