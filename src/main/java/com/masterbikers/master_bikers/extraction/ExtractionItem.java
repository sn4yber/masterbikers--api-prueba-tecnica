package com.masterbikers.master_bikers.extraction;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "extraction_items",
		uniqueConstraints = @UniqueConstraint(
				name = "extraction_items_job_external_product_unique",
				columnNames = { "extraction_job_id", "external_product_id" }))
public class ExtractionItem {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "extraction_job_id", nullable = false, updatable = false)
	private ExtractionJob extractionJob;

	@Column(name = "external_product_id", nullable = false, length = 255, updatable = false)
	private String externalProductId;

	@Column(name = "product_id")
	private UUID productId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ExtractionItemStatus status;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "processed_at")
	private Instant processedAt;

	protected ExtractionItem() {
	}

	private ExtractionItem(ExtractionJob extractionJob, String externalProductId) {
		this.id = UUID.randomUUID();
		this.extractionJob = extractionJob;
		this.externalProductId = externalProductId;
		this.status = ExtractionItemStatus.PENDING;
	}

	public static ExtractionItem create(ExtractionJob extractionJob, String externalProductId) {
		if (extractionJob == null) {
			throw new IllegalArgumentException("extractionJob is required");
		}
		if (externalProductId == null || externalProductId.isBlank()) {
			throw new IllegalArgumentException("externalProductId is required");
		}
		return new ExtractionItem(extractionJob, externalProductId);
	}

	public boolean start() {
		if (status != ExtractionItemStatus.PENDING) {
			return false;
		}
		status = ExtractionItemStatus.PROCESSING;
		return true;
	}

	public void succeed(UUID productId) {
		if (status != ExtractionItemStatus.PROCESSING) {
			return;
		}
		this.productId = productId;
		this.status = ExtractionItemStatus.SUCCESS;
		this.errorMessage = null;
		this.processedAt = Instant.now();
	}

	public void fail(String errorMessage) {
		if (status != ExtractionItemStatus.PENDING && status != ExtractionItemStatus.PROCESSING) {
			return;
		}
		this.status = ExtractionItemStatus.FAILED;
		this.errorMessage = truncate(errorMessage);
		this.processedAt = Instant.now();
	}

	private String truncate(String value) {
		if (value == null || value.isBlank()) {
			return "Product extraction failed";
		}
		return value.length() <= 1000 ? value : value.substring(0, 1000);
	}

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (status == null) {
			status = ExtractionItemStatus.PENDING;
		}
	}

	public UUID getId() {
		return id;
	}

	public ExtractionJob getExtractionJob() {
		return extractionJob;
	}

	public String getExternalProductId() {
		return externalProductId;
	}

	public UUID getProductId() {
		return productId;
	}

	public ExtractionItemStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}
}
