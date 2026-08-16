package com.masterbikers.master_bikers.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractionStateServiceTests {

	@Mock
	private ExtractionJobRepository jobRepository;

	@Mock
	private ExtractionItemRepository itemRepository;

	private ExtractionStateService stateService;

	@BeforeEach
	void setUp() {
		stateService = new ExtractionStateService(jobRepository, itemRepository);
	}

	@Test
	void resetsProcessingItemsWhenRecoveringJob() {
		ExtractionJob job = ExtractionJob.create(1);
		ExtractionItem item = ExtractionItem.create(job, "5");
		job.start();
		item.start();
		when(jobRepository.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
		when(itemRepository.findAllByJobIdForUpdate(job.getId())).thenReturn(List.of(item));

		assertTrue(stateService.recoverJob(job.getId()));

		assertEquals(ExtractionStatus.PENDING, job.getStatus());
		assertEquals(ExtractionItemStatus.PENDING, item.getStatus());
		assertNull(job.getStartedAt());
	}
}
