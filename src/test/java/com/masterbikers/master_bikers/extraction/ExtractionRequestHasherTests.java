package com.masterbikers.master_bikers.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExtractionRequestHasherTests {

	private final ExtractionRequestHasher hasher = new ExtractionRequestHasher();

	@Test
	void equivalentRequestsHaveSameHashRegardlessOfOrder() {
		String first = hasher.hash(new ExtractionCreateRequest(List.of(3L, 1L, 2L)));
		String second = hasher.hash(new ExtractionCreateRequest(List.of(1L, 2L, 3L)));

		assertEquals(first, second);
		assertEquals(64, first.length());
	}

	@Test
	void differentRequestsHaveDifferentHashes() {
		String first = hasher.hash(new ExtractionCreateRequest(List.of(1L, 2L)));
		String second = hasher.hash(new ExtractionCreateRequest(List.of(1L, 3L)));

		assertNotEquals(first, second);
	}
}
