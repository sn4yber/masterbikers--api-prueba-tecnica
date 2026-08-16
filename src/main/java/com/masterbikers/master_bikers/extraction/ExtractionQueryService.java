package com.masterbikers.master_bikers.extraction;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.masterbikers.master_bikers.common.exception.ResourceNotFoundException;

@Service
public class ExtractionQueryService {

	private final ExtractionJobRepository jobRepository;
	private final ExtractionItemRepository itemRepository;

	public ExtractionQueryService(
			ExtractionJobRepository jobRepository,
			ExtractionItemRepository itemRepository) {
		this.jobRepository = jobRepository;
		this.itemRepository = itemRepository;
	}

	@Transactional(readOnly = true)
	public ExtractionStatusResponse getStatus(UUID jobId) {
		ExtractionJob job = findJob(jobId);
		long successful = itemRepository.countByExtractionJobIdAndStatus(jobId, ExtractionItemStatus.SUCCESS);
		long failed = itemRepository.countByExtractionJobIdAndStatus(jobId, ExtractionItemStatus.FAILED);
		return new ExtractionStatusResponse(
				job.getId(),
				job.getStatus(),
				job.getTotal(),
				successful + failed,
				successful,
				failed,
				job.getCreatedAt(),
				job.getStartedAt(),
				job.getFinishedAt());
	}

	@Transactional(readOnly = true)
	public List<ExtractionItemResponse> getItems(UUID jobId) {
		findJob(jobId);
		return itemRepository.findAllByJobId(jobId).stream()
				.map(item -> new ExtractionItemResponse(
						item.getExternalProductId(),
						item.getStatus(),
						item.getProductId(),
						item.getErrorMessage(),
						item.getProcessedAt()))
				.toList();
	}

	private ExtractionJob findJob(UUID jobId) {
		return jobRepository.findById(jobId)
				.orElseThrow(() -> new ResourceNotFoundException("Extraction " + jobId + " was not found"));
	}
}
