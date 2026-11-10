package com.duocuc.sistema_tickets.service;

import com.duocuc.sistema_tickets.model.Ticket;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TicketPdfGenerator {

	private final Path ticketsDirectory;

	public TicketPdfGenerator(@Value("${app.storage.efs-directory:./data/tickets}") String directory) {
		this.ticketsDirectory = Path.of(directory).toAbsolutePath().normalize();
	}

	public Path generate(Ticket ticket) {
		Path file = pathFor(ticket);
		try {
			Files.createDirectories(ticketsDirectory);
			try (PDDocument document = new PDDocument()) {
				PDPage page = new PDPage();
				document.addPage(page);
				try (PDPageContentStream content = new PDPageContentStream(document, page)) {
					content.beginText();
					content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
					content.newLineAtOffset(50, 740);
					content.showText("Entrada para " + ticket.getEventName());
					content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
					content.newLineAtOffset(0, -35);
					content.showText("Ticket: " + ticket.getId());
					content.newLineAtOffset(0, -20);
					content.showText("Asistente: " + ticket.getAttendeeName());
					content.newLineAtOffset(0, -20);
					content.showText("Usuario: " + ticket.getUserId());
					content.newLineAtOffset(0, -20);
					content.showText("Evento: " + ticket.getEventId());
					content.newLineAtOffset(0, -20);
					content.showText("Precio: " + ticket.getUnitPrice().toPlainString());
					content.newLineAtOffset(0, -20);
					content.showText("Detalle: " + ticket.getDetails());
					content.endText();
				}
				document.save(file.toFile());
			}
			return file;
		} catch (IOException | IllegalArgumentException exception) {
			throw new TicketFileException("No se pudo generar el PDF del ticket " + ticket.getId(), exception);
		}
	}

	public Path pathFor(Ticket ticket) {
		return ticketsDirectory.resolve(ticket.getId() + ".pdf").normalize();
	}

	public void delete(Ticket ticket) {
		try {
			Files.deleteIfExists(pathFor(ticket));
		} catch (IOException exception) {
			throw new TicketFileException("No se pudo eliminar el archivo temporal del ticket " + ticket.getId(),
					exception);
		}
	}
}
