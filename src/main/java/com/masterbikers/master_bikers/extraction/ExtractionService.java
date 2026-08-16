package com.masterbikers.master_bikers.extraction;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {

	private final ExtractionJobRepository jobRepository;
	private final ExtractionItemRepository itemRepository;
	private final ApplicationEventPublisher eventPublisher;

	public ExtractionService(
			ExtractionJobRepository jobRepository,
			ExtractionItemRepository itemRepository,
			ApplicationEventPublisher eventPublisher) {
		this.jobRepository = jobRepository;
		this.itemRepository = itemRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public ExtractionCreatedResponse create(ExtractionCreateRequest request) {
		ExtractionJob job = jobRepository.save(ExtractionJob.create(request.productIds().size()));
		List<ExtractionItem> items = request.productIds().stream()
				.map(productId -> ExtractionItem.create(job, productId.toString()))
				.toList();
		itemRepository.saveAll(items);
		eventPublisher.publishEvent(new ExtractionCreatedEvent(job.getId()));
		return new ExtractionCreatedResponse(job.getId(), job.getStatus());
	}
}
