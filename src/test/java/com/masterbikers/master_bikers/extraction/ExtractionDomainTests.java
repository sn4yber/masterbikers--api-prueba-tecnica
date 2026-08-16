package com.masterbikers.master_bikers.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ExtractionDomainTests {

	@Test
	void completesJobWhenEveryItemSucceeds() {
		ExtractionJob job = ExtractionJob.create(2);

		assertTrue(job.start());
		job.finish(2, 0);

		assertEquals(ExtractionStatus.COMPLETED, job.getStatus());
		assertNotNull(job.getStartedAt());
		assertNotNull(job.getFinishedAt());
	}

	@Test
	void completesJobWithErrorsWhenResultsAreMixed() {
		ExtractionJob job = ExtractionJob.create(2);
		job.start();

		job.finish(1, 1);

		assertEquals(ExtractionStatus.COMPLETED_WITH_ERRORS, job.getStatus());
	}

	@Test
	void failsJobWhenEveryItemFailsOrResultsAreUnresolved() {
		ExtractionJob failed = ExtractionJob.create(2);
		failed.start();
		failed.finish(0, 2);
		ExtractionJob unresolved = ExtractionJob.create(2);
		unresolved.start();
		unresolved.finish(1, 0);

		assertEquals(ExtractionStatus.FAILED, failed.getStatus());
		assertEquals(ExtractionStatus.FAILED, unresolved.getStatus());
	}

	@Test
	void itemTransitionsOnceAndTruncatesFailureMessage() {
		ExtractionJob job = ExtractionJob.create(1);
		ExtractionItem item = ExtractionItem.create(job, "7");

		assertTrue(item.start());
		assertFalse(item.start());
		item.fail("x".repeat(1200));

		assertEquals(ExtractionItemStatus.FAILED, item.getStatus());
		assertEquals(1000, item.getErrorMessage().length());
		assertNotNull(item.getProcessedAt());
		assertNull(item.getProductId());
	}

	@Test
	void successfulItemLinksProduct() {
		ExtractionItem item = ExtractionItem.create(ExtractionJob.create(1), "8");
		UUID productId = UUID.randomUUID();
		item.start();

		item.succeed(productId);

		assertEquals(ExtractionItemStatus.SUCCESS, item.getStatus());
		assertEquals(productId, item.getProductId());
		assertNull(item.getErrorMessage());
	}

	@Test
	void resetsInterruptedJobAndItemForRecovery() {
		ExtractionJob job = ExtractionJob.create(1);
		ExtractionItem item = ExtractionItem.create(job, "9");
		job.start();
		item.start();

		assertTrue(job.recover());
		item.recover();

		assertEquals(ExtractionStatus.PENDING, job.getStatus());
		assertNull(job.getStartedAt());
		assertEquals(ExtractionItemStatus.PENDING, item.getStatus());
		assertNull(item.getProcessedAt());
	}
}
