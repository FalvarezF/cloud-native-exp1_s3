package com.duocuc.sistema_tickets.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record GenerateTicketsRequest(
		@NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,80}$") String eventId,
		@NotBlank @Size(max = 160) String eventName,
		@NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,80}$") String userId,
		@NotBlank @Size(max = 160) String attendeeName,
		@NotBlank @Size(max = 1000) String details,
		@NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice,
		@Positive @Max(100) int quantity) {
}
