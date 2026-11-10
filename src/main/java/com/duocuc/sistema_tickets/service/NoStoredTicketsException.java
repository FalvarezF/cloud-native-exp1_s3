package com.duocuc.sistema_tickets.service;

public class NoStoredTicketsException extends RuntimeException {

	public NoStoredTicketsException() {
		super("No hay tickets subidos a S3 para los filtros indicados");
	}
}
