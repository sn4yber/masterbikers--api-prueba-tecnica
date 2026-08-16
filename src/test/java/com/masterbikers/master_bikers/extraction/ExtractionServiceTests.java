package com.masterbikers.master_bikers.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ExtractionServiceTests {

	@Mock
	private ExtractionJobRepository jobRepository;

	@Mock
	private ExtractionItemRepository itemRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private ExtractionService service;

	@BeforeEach
	void setUp() {
		service = new ExtractionService(jobRepository, itemRepository, eventPublisher);
	}

	@Test
	void createsJobItemsAndPublishesEvent() {
		when(jobRepository.save(any(ExtractionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ExtractionCreatedResponse response = service.create(new ExtractionCreateRequest(List.of(3L, 1L)));

		assertEquals(ExtractionStatus.PENDING, response.status());
		verify(itemRepository).saveAll(any());
		ArgumentCaptor<ExtractionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ExtractionCreatedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertEquals(response.id(), eventCaptor.getValue().jobId());
	}
}
