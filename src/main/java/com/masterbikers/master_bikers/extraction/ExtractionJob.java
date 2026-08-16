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

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	protected ExtractionJob() {
	}

	private ExtractionJob(int total) {
		if (total <= 0) {
			throw new IllegalArgumentException("total must be positive");
		}
		this.id = UUID.randomUUID();
		this.status = ExtractionStatus.PENDING;
		this.total = total;
		this.createdAt = Instant.now();
	}

	public static ExtractionJob create(int total) {
		return new ExtractionJob(total);
	}

	public boolean start() {
		if (status != ExtractionStatus.PENDING) {
			return false;
		}
		status = ExtractionStatus.PROCESSING;
		startedAt = Instant.now();
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
