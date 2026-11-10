package com.duocuc.sistema_tickets.controller;

import com.duocuc.sistema_tickets.model.Ticket;
import com.duocuc.sistema_tickets.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService service;

	public TicketController(TicketService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<List<Ticket>> generate(@Valid @RequestBody GenerateTicketsRequest request) {
		return ResponseEntity.status(201).body(service.generate(request));
	}

	@GetMapping
	public List<Ticket> list(
			@RequestParam(required = false) String eventId,
			@RequestParam(required = false) String userId) {
		return service.list(eventId, userId);
	}

	@GetMapping("/stats")
	public List<EventSalesStatsResponse> stats(@RequestParam(required = false) String eventId) {
		return service.stats(eventId);
	}

	@GetMapping("/download")
	public ResponseEntity<byte[]> downloadByFilter(
			@RequestParam(required = false) String eventId,
			@RequestParam(required = false) String userId) {
		if (eventId == null && userId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica eventId y/o userId");
		}
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("tickets.zip").build().toString())
				.body(service.downloadAsZip(eventId, userId));
	}

	@GetMapping("/{id}")
	public Ticket get(@PathVariable String id) {
		return service.get(id);
	}

	@PostMapping("/{id}/upload")
	public Ticket upload(@PathVariable String id) {
		return service.upload(id);
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<byte[]> download(@PathVariable String id) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(id + ".pdf").build().toString())
				.body(service.download(id));
	}

	@GetMapping("/{id}/versions")
	public List<TicketVersionResponse> versions(@PathVariable String id) {
		return service.versions(id);
	}

	@PutMapping("/{id}")
	public Ticket update(@PathVariable String id, @Valid @RequestBody UpdateTicketRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
