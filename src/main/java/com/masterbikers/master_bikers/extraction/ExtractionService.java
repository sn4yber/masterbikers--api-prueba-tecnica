package com.masterbikers.master_bikers.extraction;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ExtractionService {

	private final ExtractionJobRepository jobRepository;
	private final ExtractionCreationTransaction creationTransaction;
	private final ExtractionRequestHasher requestHasher;

	public ExtractionService(
			ExtractionJobRepository jobRepository,
			ExtractionCreationTransaction creationTransaction,
			ExtractionRequestHasher requestHasher) {
		this.jobRepository = jobRepository;
		this.creationTransaction = creationTransaction;
		this.requestHasher = requestHasher;
	}

	public ExtractionCreatedResponse create(ExtractionCreateRequest request) {
		String requestHash = requestHasher.hash(request);
		return jobRepository.findByRequestHash(requestHash)
				.map(this::toResponse)
				.orElseGet(() -> createSafely(request, requestHash));
	}

	private ExtractionCreatedResponse createSafely(ExtractionCreateRequest request, String requestHash) {
		try {
			return creationTransaction.create(request, requestHash);
		}
		catch (DataIntegrityViolationException exception) {
			return jobRepository.findByRequestHash(requestHash)
					.map(this::toResponse)
					.orElseThrow(() -> exception);
		}
	}

	private ExtractionCreatedResponse toResponse(ExtractionJob job) {
		return new ExtractionCreatedResponse(job.getId(), job.getStatus());
	}
}
