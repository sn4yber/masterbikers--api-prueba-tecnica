package com.masterbikers.master_bikers.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractionQueryServiceTests {

	@Mock
	private ExtractionJobRepository jobRepository;

	@Mock
	private ExtractionItemRepository itemRepository;

	private ExtractionQueryService service;

	@BeforeEach
	void setUp() {
		service = new ExtractionQueryService(jobRepository, itemRepository);
	}

	@Test
	void computesProgressFromPersistedItemStatuses() {
		ExtractionJob job = ExtractionJob.create(5);
		UUID jobId = job.getId();
		when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
		when(itemRepository.countByExtractionJobIdAndStatus(jobId, ExtractionItemStatus.SUCCESS)).thenReturn(2L);
		when(itemRepository.countByExtractionJobIdAndStatus(jobId, ExtractionItemStatus.FAILED)).thenReturn(1L);

		ExtractionStatusResponse response = service.getStatus(jobId);

		assertEquals(3, response.processed());
		assertEquals(2, response.successful());
		assertEquals(1, response.failed());
	}
}
