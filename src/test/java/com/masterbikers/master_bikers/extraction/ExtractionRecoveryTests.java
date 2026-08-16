package com.masterbikers.master_bikers.extraction;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ExtractionRecoveryTests {

	@Mock
	private ExtractionJobRepository jobRepository;

	@Mock
	private ExtractionStateService stateService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private ExtractionRecovery recovery;

	@BeforeEach
	void setUp() {
		recovery = new ExtractionRecovery(jobRepository, stateService, eventPublisher);
	}

	@Test
	void republishesRecoverableJobs() {
		UUID recoverable = UUID.randomUUID();
		UUID alreadyClaimed = UUID.randomUUID();
		when(jobRepository.findIdsByStatusIn(anyCollection())).thenReturn(List.of(recoverable, alreadyClaimed));
		when(stateService.recoverJob(recoverable)).thenReturn(true);
		when(stateService.recoverJob(alreadyClaimed)).thenReturn(false);

		recovery.recoverInterruptedJobs();

		verify(eventPublisher).publishEvent(new ExtractionCreatedEvent(recoverable));
		verify(eventPublisher, never()).publishEvent(new ExtractionCreatedEvent(alreadyClaimed));
	}
}
