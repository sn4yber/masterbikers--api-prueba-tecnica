package com.masterbikers.master_bikers.extraction;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionCreationTransaction {

	private final ExtractionJobRepository jobRepository;
	private final ExtractionItemRepository itemRepository;
	private final ApplicationEventPublisher eventPublisher;

	public ExtractionCreationTransaction(
			ExtractionJobRepository jobRepository,
			ExtractionItemRepository itemRepository,
			ApplicationEventPublisher eventPublisher) {
		this.jobRepository = jobRepository;
		this.itemRepository = itemRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public ExtractionCreatedResponse create(ExtractionCreateRequest request, String requestHash) {
		return jobRepository.findByRequestHash(requestHash)
				.map(this::toResponse)
				.orElseGet(() -> createNew(request, requestHash));
	}

	private ExtractionCreatedResponse createNew(ExtractionCreateRequest request, String requestHash) {
		ExtractionJob job = jobRepository.saveAndFlush(
				ExtractionJob.create(request.productIds().size(), requestHash));
		List<ExtractionItem> items = request.productIds().stream()
				.map(productId -> ExtractionItem.create(job, productId.toString()))
				.toList();
		itemRepository.saveAll(items);
		eventPublisher.publishEvent(new ExtractionCreatedEvent(job.getId()));
		return toResponse(job);
	}

	private ExtractionCreatedResponse toResponse(ExtractionJob job) {
		return new ExtractionCreatedResponse(job.getId(), job.getStatus());
	}
}
