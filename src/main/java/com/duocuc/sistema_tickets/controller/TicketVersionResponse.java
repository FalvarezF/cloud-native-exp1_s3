package com.duocuc.sistema_tickets.controller;

import java.time.Instant;

public record TicketVersionResponse(
		String versionId,
		Instant lastModified,
		boolean latest,
		boolean deleteMarker) {
}
