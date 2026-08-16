package com.masterbikers.master_bikers.common.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException exception) {
		return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.computeIfAbsent(error.getField(), key -> new ArrayList<>())
						.add(error.getDefaultMessage()));
		detail.setProperty("fieldErrors", fieldErrors);
		return detail;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
		ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		exception.getConstraintViolations().forEach(violation ->
				fieldErrors.computeIfAbsent(violation.getPropertyPath().toString(), key -> new ArrayList<>())
						.add(violation.getMessage()));
		detail.setProperty("fieldErrors", fieldErrors);
		return detail;
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	ProblemDetail handleMalformedRequest(Exception exception) {
		return problem(HttpStatus.BAD_REQUEST, "Malformed request", "The request contains malformed JSON or an invalid value type");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ProblemDetail handleMissingRoute(NoResourceFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", "The requested route does not exist");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		log.error("Unexpected request failure", exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
