package com.masterbikers.master_bikers.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractionServiceTests {

	private static final String REQUEST_HASH = "a".repeat(64);

	@Mock
	private ExtractionJobRepository jobRepository;

	@Mock
	private ExtractionCreationTransaction creationTransaction;

	@Mock
	private ExtractionRequestHasher requestHasher;

	private ExtractionService service;

	@BeforeEach
	void setUp() {
		service = new ExtractionService(jobRepository, creationTransaction, requestHasher);
	}

	@Test
	void createsJobWhenEquivalentRequestDoesNotExist() {
		ExtractionCreateRequest request = new ExtractionCreateRequest(List.of(3L, 1L));
		ExtractionJob job = ExtractionJob.create(2, REQUEST_HASH);
		ExtractionCreatedResponse created = new ExtractionCreatedResponse(job.getId(), job.getStatus());
		when(requestHasher.hash(request)).thenReturn(REQUEST_HASH);
		when(jobRepository.findByRequestHash(REQUEST_HASH)).thenReturn(Optional.empty());
		when(creationTransaction.create(request, REQUEST_HASH)).thenReturn(created);

		ExtractionCreatedResponse response = service.create(request);

		assertEquals(created, response);
		verify(creationTransaction).create(request, REQUEST_HASH);
	}

	@Test
	void returnsExistingJobForEquivalentRequest() {
		ExtractionCreateRequest request = new ExtractionCreateRequest(List.of(1L, 3L));
		ExtractionJob existing = ExtractionJob.create(2, REQUEST_HASH);
		when(requestHasher.hash(request)).thenReturn(REQUEST_HASH);
		when(jobRepository.findByRequestHash(REQUEST_HASH)).thenReturn(Optional.of(existing));

		ExtractionCreatedResponse response = service.create(request);

		assertEquals(existing.getId(), response.id());
		verify(creationTransaction, never()).create(request, REQUEST_HASH);
	}
}
