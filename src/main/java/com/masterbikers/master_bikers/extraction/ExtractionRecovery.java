package com.masterbikers.master_bikers.extraction;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ExtractionRecovery {

	private static final Logger log = LoggerFactory.getLogger(ExtractionRecovery.class);
	private static final List<ExtractionStatus> RECOVERABLE_STATUSES = List.of(
			ExtractionStatus.PENDING,
			ExtractionStatus.PROCESSING);

	private final ExtractionJobRepository jobRepository;
	private final ExtractionStateService stateService;
	private final ApplicationEventPublisher eventPublisher;

	public ExtractionRecovery(
			ExtractionJobRepository jobRepository,
			ExtractionStateService stateService,
			ApplicationEventPublisher eventPublisher) {
		this.jobRepository = jobRepository;
		this.stateService = stateService;
		this.eventPublisher = eventPublisher;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void recoverInterruptedJobs() {
		List<UUID> jobIds = jobRepository.findIdsByStatusIn(RECOVERABLE_STATUSES);
		int recovered = 0;
		for (UUID jobId : jobIds) {
			if (stateService.recoverJob(jobId)) {
				eventPublisher.publishEvent(new ExtractionCreatedEvent(jobId));
				recovered++;
			}
		}
		if (recovered > 0) {
			log.info("Scheduled {} interrupted extraction job(s) for recovery", recovered);
		}
	}
}
