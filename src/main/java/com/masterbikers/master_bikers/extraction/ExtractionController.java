package com.masterbikers.master_bikers.extraction;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/extractions")
public class ExtractionController {

	private final ExtractionService extractionService;
	private final ExtractionQueryService queryService;

	public ExtractionController(ExtractionService extractionService, ExtractionQueryService queryService) {
		this.extractionService = extractionService;
		this.queryService = queryService;
	}

	@PostMapping
	public ResponseEntity<ExtractionCreatedResponse> create(
			@Valid @RequestBody ExtractionCreateRequest request) {
		ExtractionCreatedResponse extraction = extractionService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(extraction.id())
				.toUri();
		return ResponseEntity.accepted().location(location).body(extraction);
	}

	@GetMapping("/{id}")
	public ExtractionStatusResponse getStatus(@PathVariable UUID id) {
		return queryService.getStatus(id);
	}

	@GetMapping("/{id}/items")
	public List<ExtractionItemResponse> getItems(@PathVariable UUID id) {
		return queryService.getItems(id);
	}
}
