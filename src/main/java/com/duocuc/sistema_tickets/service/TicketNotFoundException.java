package com.duocuc.sistema_tickets.service;

public class TicketNotFoundException extends RuntimeException {

	public TicketNotFoundException(String id) {
		super("No existe un ticket con id " + id);
	}
}
