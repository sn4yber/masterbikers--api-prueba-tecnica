package com.masterbikers.master_bikers.extraction;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "extraction_jobs")
public class ExtractionJob {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ExtractionStatus status;

	@Column(name = "total", nullable = false)
	private int total;

	@Column(name = "request_hash", nullable = false, updatable = false, length = 64, unique = true)
	private String requestHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	protected ExtractionJob() {
	}

	private ExtractionJob(int total, String requestHash) {
		if (total <= 0) {
			throw new IllegalArgumentException("total must be positive");
		}
		if (requestHash == null || requestHash.length() != 64) {
			throw new IllegalArgumentException("requestHash must contain 64 characters");
		}
		this.id = UUID.randomUUID();
		this.status = ExtractionStatus.PENDING;
		this.total = total;
		this.requestHash = requestHash;
		this.createdAt = Instant.now();
	}

	public static ExtractionJob create(int total, String requestHash) {
		return new ExtractionJob(total, requestHash);
	}

	public static ExtractionJob create(int total) {
		String requestHash = UUID.randomUUID().toString().replace("-", "")
				+ UUID.randomUUID().toString().replace("-", "");
		return new ExtractionJob(total, requestHash);
	}

	public boolean start() {
		if (status != ExtractionStatus.PENDING) {
			return false;
		}
		status = ExtractionStatus.PROCESSING;
		startedAt = Instant.now();
		return true;
	}

	public boolean recover() {
		if (status != ExtractionStatus.PENDING && status != ExtractionStatus.PROCESSING) {
			return false;
		}
		status = ExtractionStatus.PENDING;
		startedAt = null;
		finishedAt = null;
		return true;
	}

	public void finish(long successful, long failed) {
		if (status != ExtractionStatus.PROCESSING) {
			return;
		}
		if (successful == total) {
			status = ExtractionStatus.COMPLETED;
		}
		else if (failed == total) {
			status = ExtractionStatus.FAILED;
		}
		else if (successful + failed == total && successful > 0 && failed > 0) {
			status = ExtractionStatus.COMPLETED_WITH_ERRORS;
		}
		else {
			status = ExtractionStatus.FAILED;
		}
		finishedAt = Instant.now();
	}

	public void fail() {
		if (status == ExtractionStatus.COMPLETED || status == ExtractionStatus.COMPLETED_WITH_ERRORS) {
			return;
		}
		Instant now = Instant.now();
		if (startedAt == null) {
			startedAt = now;
		}
		status = ExtractionStatus.FAILED;
		finishedAt = now;
	}

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (status == null) {
			status = ExtractionStatus.PENDING;
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public ExtractionStatus getStatus() {
		return status;
	}

	public int getTotal() {
		return total;
	}

	public String getRequestHash() {
		return requestHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}
}
