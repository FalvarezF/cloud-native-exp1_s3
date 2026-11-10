package com.duocuc.sistema_tickets.repository.projection;

import java.math.BigDecimal;

public interface EventSalesStats {

	String getEventId();

	long getTicketsGenerated();

	long getTicketsUploaded();

	BigDecimal getTotalSales();
}
