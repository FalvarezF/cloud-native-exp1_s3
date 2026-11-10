package com.duocuc.sistema_tickets.service;

public class S3StorageNotConfiguredException extends RuntimeException {

	public S3StorageNotConfiguredException() {
		super("Configura la variable S3_BUCKET para habilitar el almacenamiento S3");
	}
}
