package com.duocuc.sistema_tickets.service;

import com.duocuc.sistema_tickets.controller.EventSalesStatsResponse;
import com.duocuc.sistema_tickets.controller.GenerateTicketsRequest;
import com.duocuc.sistema_tickets.controller.TicketVersionResponse;
import com.duocuc.sistema_tickets.controller.UpdateTicketRequest;
import com.duocuc.sistema_tickets.model.Ticket;
import com.duocuc.sistema_tickets.repository.TicketRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

	private final TicketRepository repository;
	private final TicketPdfGenerator pdfGenerator;
	private final TicketS3StorageService s3Storage;
	private final boolean autoUpload;

	public TicketService(TicketRepository repository, TicketPdfGenerator pdfGenerator,
			TicketS3StorageService s3Storage,
			@Value("${app.aws.s3.auto-upload:false}") boolean autoUpload) {
		this.repository = repository;
		this.pdfGenerator = pdfGenerator;
		this.s3Storage = s3Storage;
		this.autoUpload = autoUpload;
	}

	public List<Ticket> generate(GenerateTicketsRequest request) {
		List<Ticket> tickets = java.util.stream.IntStream.range(0, request.quantity())
				.mapToObj(index -> new Ticket(request.eventId(), request.eventName(), request.userId(),
						request.attendeeName(), request.details(), request.unitPrice()))
				.toList();

		for (Ticket ticket : tickets) {
			pdfGenerator.generate(ticket);
		}
		List<Ticket> savedTickets = repository.saveAllAndFlush(tickets);
		if (autoUpload) {
			savedTickets = savedTickets.stream().map(this::store).toList();
		}
		return savedTickets;
	}

	@Transactional(readOnly = true)
	public Ticket get(String id) {
		return findTicket(id);
	}

	@Transactional(readOnly = true)
	public List<Ticket> list(String eventId, String userId) {
		if (eventId != null && userId != null) {
			return repository.findByEventIdAndUserId(eventId, userId);
		}
		if (eventId != null) {
			return repository.findByEventId(eventId);
		}
		if (userId != null) {
			return repository.findByUserId(userId);
		}
		return repository.findAll();
	}

	public Ticket upload(String id) {
		return store(findTicket(id));
	}

	private Ticket store(Ticket ticket) {
		Path file = pdfGenerator.pathFor(ticket);
		if (!Files.exists(file)) {
			file = pdfGenerator.generate(ticket);
		}
		String versionId = s3Storage.upload(ticket, file);
		ticket.markStored(versionId);
		return repository.save(ticket);
	}

	public Ticket update(String id, UpdateTicketRequest request) {
		Ticket ticket = findTicket(id);
		ticket.updateDetails(request.attendeeName(), request.details());
		Path file = pdfGenerator.generate(ticket);
		if (ticket.getStatus() == com.duocuc.sistema_tickets.model.TicketStatus.STORED) {
			ticket.markStored(s3Storage.upload(ticket, file));
		}
		return repository.save(ticket);
	}

	public byte[] download(String id) {
		Ticket ticket = findTicket(id);
		if (ticket.getStatus() != com.duocuc.sistema_tickets.model.TicketStatus.STORED) {
			throw new IllegalStateException("El ticket aún no se ha subido a S3");
		}
		return s3Storage.download(ticket);
	}

	@Transactional(readOnly = true)
	public byte[] downloadAsZip(String eventId, String userId) {
		List<Ticket> tickets = list(eventId, userId).stream()
				.filter(ticket -> ticket.getStatus() == com.duocuc.sistema_tickets.model.TicketStatus.STORED)
				.toList();
		if (tickets.isEmpty()) {
			throw new NoStoredTicketsException();
		}

		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(bytes)) {
			for (Ticket ticket : tickets) {
				zip.putNextEntry(new ZipEntry(ticket.getId() + ".pdf"));
				zip.write(s3Storage.download(ticket));
				zip.closeEntry();
			}
			zip.finish();
			return bytes.toByteArray();
		} catch (IOException exception) {
			throw new TicketFileException("No se pudieron empaquetar los tickets solicitados", exception);
		}
	}

	public List<TicketVersionResponse> versions(String id) {
		return s3Storage.versions(findTicket(id));
	}

	public void delete(String id) {
		Ticket ticket = findTicket(id);
		if (ticket.getStatus() == com.duocuc.sistema_tickets.model.TicketStatus.STORED) {
			s3Storage.delete(ticket);
		}
		pdfGenerator.delete(ticket);
		repository.delete(ticket);
	}

	@Transactional(readOnly = true)
	public List<EventSalesStatsResponse> stats(String eventId) {
		return repository.getSalesStats(eventId).stream()
				.map(stats -> new EventSalesStatsResponse(
						stats.getEventId(),
						stats.getTicketsGenerated(),
						stats.getTicketsUploaded(),
						stats.getTotalSales()))
				.toList();
	}

	private Ticket findTicket(String id) {
		return repository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
	}
}
