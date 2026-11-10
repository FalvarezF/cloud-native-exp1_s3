package com.duocuc.sistema_tickets.controller;

import com.duocuc.sistema_tickets.service.TicketFileException;
import com.duocuc.sistema_tickets.service.TicketNotFoundException;
import com.duocuc.sistema_tickets.service.NoStoredTicketsException;
import com.duocuc.sistema_tickets.service.S3StorageNotConfiguredException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(TicketNotFoundException.class)
	public ResponseEntity<ApiError> notFound(TicketNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(NoStoredTicketsException.class)
	public ResponseEntity<ApiError> noStoredTickets(NoStoredTicketsException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> invalidState(IllegalStateException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(TicketFileException.class)
	public ResponseEntity<ApiError> fileError(TicketFileException exception) {
		logger.error("Falló una operación con el archivo de un ticket", exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	@ExceptionHandler(S3StorageNotConfiguredException.class)
	public ResponseEntity<ApiError> storageNotConfigured(S3StorageNotConfiguredException exception) {
		return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	@ExceptionHandler({S3Exception.class, SdkClientException.class})
	public ResponseEntity<ApiError> awsError(RuntimeException exception) {
		logger.error("Falló una operación con AWS S3", exception);
		return error(HttpStatus.BAD_GATEWAY, "Falló la operación con AWS S3");
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), message));
	}

	public record ApiError(Instant timestamp, int status, String message) {
	}
}
