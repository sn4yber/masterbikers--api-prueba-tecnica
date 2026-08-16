package com.masterbikers.master_bikers.extraction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionStateService {

	private final ExtractionJobRepository jobRepository;
	private final ExtractionItemRepository itemRepository;

	public ExtractionStateService(
			ExtractionJobRepository jobRepository,
			ExtractionItemRepository itemRepository) {
		this.jobRepository = jobRepository;
		this.itemRepository = itemRepository;
	}

	@Transactional
	public boolean startJob(UUID jobId) {
		return jobRepository.findByIdForUpdate(jobId)
				.map(ExtractionJob::start)
				.orElse(false);
	}

	@Transactional(readOnly = true)
	public List<UUID> getItemIds(UUID jobId) {
		return itemRepository.findIdsByJobId(jobId);
	}

	@Transactional
	public Optional<String> startItem(UUID itemId) {
		return itemRepository.findByIdForUpdate(itemId)
				.filter(ExtractionItem::start)
				.map(ExtractionItem::getExternalProductId);
	}

	@Transactional
	public void succeedItem(UUID itemId, UUID productId) {
		itemRepository.findById(itemId).ifPresent(item -> item.succeed(productId));
	}

	@Transactional
	public void failItem(UUID itemId, String safeMessage) {
		itemRepository.findById(itemId).ifPresent(item -> item.fail(safeMessage));
	}

	@Transactional
	public void finishJob(UUID jobId) {
		jobRepository.findByIdForUpdate(jobId).ifPresent(job -> {
			long successful = itemRepository.countByExtractionJobIdAndStatus(jobId, ExtractionItemStatus.SUCCESS);
			long failed = itemRepository.countByExtractionJobIdAndStatus(jobId, ExtractionItemStatus.FAILED);
			job.finish(successful, failed);
		});
	}

	@Transactional
	public void failJob(UUID jobId) {
		jobRepository.findByIdForUpdate(jobId).ifPresent(ExtractionJob::fail);
	}
}
