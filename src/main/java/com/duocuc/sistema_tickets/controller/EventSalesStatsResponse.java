package com.duocuc.sistema_tickets.controller;

import java.math.BigDecimal;

public record EventSalesStatsResponse(
		String eventId,
		long ticketsGenerated,
		long ticketsUploaded,
		BigDecimal totalSales) {
}
