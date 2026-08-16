package com.masterbikers.master_bikers.extraction;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ExtractionOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(ExtractionOrchestrator.class);

	private final ExtractionStateService stateService;
	private final ExtractionItemProcessor itemProcessor;
	private final Executor scraperExecutor;

	public ExtractionOrchestrator(
			ExtractionStateService stateService,
			ExtractionItemProcessor itemProcessor,
			@Qualifier("scraperExecutor") Executor scraperExecutor) {
		this.stateService = stateService;
		this.itemProcessor = itemProcessor;
		this.scraperExecutor = scraperExecutor;
	}

	@Async("extractionJobExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void process(ExtractionCreatedEvent event) {
		UUID jobId = event.jobId();
		try {
			if (!stateService.startJob(jobId)) {
				return;
			}
			List<CompletableFuture<Void>> futures = stateService.getItemIds(jobId).stream()
					.map(itemId -> CompletableFuture.runAsync(() -> itemProcessor.process(itemId), scraperExecutor))
					.toList();
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
			stateService.finishJob(jobId);
		}
		catch (Exception exception) {
			log.error("Extraction job {} failed", jobId, exception);
			try {
				stateService.failJob(jobId);
			}
			catch (Exception persistenceException) {
				log.error("Extraction job {} failure state could not be persisted", jobId, persistenceException);
			}
		}
	}
}
