package com.masterbikers.master_bikers.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductRequestValidationTests {

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
	void acceptsValidCreateRequest() {
		ProductCreateRequest request = new ProductCreateRequest(
				"Road Bike", null, new BigDecimal("10.00"), null, null, null, null, null, null, null);

		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void rejectsInvalidPriceAndIncompleteExternalReference() {
		ProductCreateRequest request = new ProductCreateRequest(
				"Road Bike", null, BigDecimal.ZERO, null, null, null, null, null, "ext-1", null);

		assertFalse(validator.validate(request).isEmpty());
		assertTrue(validator.validate(request).stream()
				.anyMatch(violation -> violation.getPropertyPath().toString().equals("externalReferenceValid")));
	}

	@Test
	void acceptsEmptyPatchAndRejectsBlankName() {
		ProductUpdateRequest emptyRequest = new ProductUpdateRequest(
				null, null, null, null, null, null, null, null, null, null);
		ProductUpdateRequest blankNameRequest = new ProductUpdateRequest(
				"   ", null, null, null, null, null, null, null, null, null);

		assertTrue(validator.validate(emptyRequest).isEmpty());
		assertFalse(validator.validate(blankNameRequest).isEmpty());
	}
}
