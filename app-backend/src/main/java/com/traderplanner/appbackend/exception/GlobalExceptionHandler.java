package com.traderplanner.appbackend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex,
			HttpServletRequest request) {
		log.warn("Service unavailable at {}: {}", request.getRequestURI(), ex.getMessage());
		return buildErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + " " + err.getDefaultMessage()).collect(Collectors.joining("; "));
		return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		String message = ex.getConstraintViolations().stream().map(v -> v.getPropertyPath() + " " + v.getMessage())
				.collect(Collectors.joining("; "));
		return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
		String path = request.getRequestURI();
		log.error("Unexpected error at {}", path, ex);

		String message = "Unexpected error";
		if (path != null && (path.startsWith("/v3/") || path.startsWith("/swagger-ui"))) {
			StringBuilder sb = new StringBuilder();
			Throwable t = ex;
			while (t != null) {
				if (sb.length() > 0)
					sb.append(" | Caused by: ");
				sb.append(t.getClass().getSimpleName()).append(": ");
				sb.append(t.getMessage() != null ? t.getMessage() : "(no message)");
				t = t.getCause();
			}
			message = sb.toString();
		}
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, path);
	}

	private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message, String path) {
		ApiErrorResponse body = new ApiErrorResponse(OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
				message, path);
		return ResponseEntity.status(status).body(body);
	}
}