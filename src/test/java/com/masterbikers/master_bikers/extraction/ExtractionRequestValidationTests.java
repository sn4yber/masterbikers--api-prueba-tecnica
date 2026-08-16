package com.masterbikers.master_bikers.extraction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.LongStream;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExtractionRequestValidationTests {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void acceptsUniquePositiveProductIds() {
		assertTrue(validator.validate(new ExtractionCreateRequest(List.of(1L, 2L, 3L))).isEmpty());
	}

	@Test
	void rejectsEmptyProductIds() {
		assertFalse(validator.validate(new ExtractionCreateRequest(List.of())).isEmpty());
	}

	@Test
	void rejectsMoreThanFiftyProductIds() {
		List<Long> ids = LongStream.rangeClosed(1, 51).boxed().toList();

		assertFalse(validator.validate(new ExtractionCreateRequest(ids)).isEmpty());
	}

	@Test
	void rejectsNonPositiveAndDuplicateProductIds() {
		assertFalse(validator.validate(new ExtractionCreateRequest(List.of(1L, 1L, 0L))).isEmpty());
	}
}
