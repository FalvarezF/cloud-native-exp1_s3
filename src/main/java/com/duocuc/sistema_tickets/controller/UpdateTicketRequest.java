package com.duocuc.sistema_tickets.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
		@NotBlank @Size(max = 160) String attendeeName,
		@NotBlank @Size(max = 1000) String details) {
}
