package com.duocuc.sistema_tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duocuc.sistema_tickets.controller.GenerateTicketsRequest;
import com.duocuc.sistema_tickets.controller.UpdateTicketRequest;
import com.duocuc.sistema_tickets.model.TicketStatus;
import com.duocuc.sistema_tickets.repository.TicketRepository;
import com.duocuc.sistema_tickets.service.TicketPdfGenerator;
import com.duocuc.sistema_tickets.service.TicketService;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.core.ResponseBytes;

@SpringBootTest(properties = {
		"app.storage.efs-directory=target/test-tickets",
		"app.aws.s3.bucket=test-bucket",
		"app.aws.s3.auto-upload=false"
})
@AutoConfigureMockMvc
class SistemaTicketsApplicationTests {

	private static final Path TICKET_DIRECTORY = Path.of("target/test-tickets");

	@Autowired
	private TicketService service;

	@Autowired
	private TicketRepository repository;

	@Autowired
	private TicketPdfGenerator pdfGenerator;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private S3Client s3Client;

	@BeforeEach
	void cleanTickets() throws Exception {
		repository.deleteAll();
		if (Files.exists(TICKET_DIRECTORY)) {
			try (var files = Files.list(TICKET_DIRECTORY)) {
				files.forEach(file -> {
					try {
						Files.deleteIfExists(file);
					} catch (Exception exception) {
						throw new IllegalStateException(exception);
					}
				});
			}
		}
	}

	@Test
	void generatesIndividualTicketsAndPdfFilesInConfiguredDirectory() {
		var tickets = service.generate(new GenerateTicketsRequest(
				"event-123", "Concierto", "user-456", "Ana Perez",
				"Acceso general", new BigDecimal("25.50"), 2));

		assertEquals(2, tickets.size());
		assertTrue(Files.exists(pdfGenerator.pathFor(tickets.get(0))));
		assertTrue(Files.exists(pdfGenerator.pathFor(tickets.get(1))));
		assertEquals(TicketStatus.GENERATED, tickets.get(0).getStatus());
		assertEquals(2, service.stats("event-123").get(0).ticketsGenerated());
		assertEquals(0, service.stats("event-123").get(0).ticketsUploaded());
	}

	@Test
	void uploadsDownloadsAndUpdatesStoredTicket() throws IOException {
		var ticket = service.generate(new GenerateTicketsRequest(
				"event-123", "Concierto", "user-456", "Ana Perez",
				"Acceso general", new BigDecimal("25.50"), 1)).get(0);
		when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenReturn(PutObjectResponse.builder().versionId("version-1").build());
		byte[] storedPdf = Files.readAllBytes(pdfGenerator.pathFor(ticket));
		when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
				.thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), storedPdf));

		var uploaded = service.upload(ticket.getId());
		assertEquals(TicketStatus.STORED, uploaded.getStatus());
		assertEquals("version-1", uploaded.getS3VersionId());
		assertEquals(storedPdf.length, service.download(ticket.getId()).length);
		try (ZipInputStream zip = new ZipInputStream(
				new ByteArrayInputStream(service.downloadAsZip("event-123", "user-456")))) {
			assertEquals(ticket.getId() + ".pdf", zip.getNextEntry().getName());
		}
		assertEquals(new BigDecimal("25.50"), service.stats("event-123").get(0).totalSales());

		service.update(ticket.getId(), new UpdateTicketRequest("Ana Actualizada", "Acceso VIP"));
		assertEquals("Ana Actualizada", service.get(ticket.getId()).getAttendeeName());
	}

	@Test
	void restApiGeneratesAndListsTickets() throws Exception {
		mockMvc.perform(post("/api/tickets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "eventId": "event-api",
								  "eventName": "Concierto",
								  "userId": "user-api",
								  "attendeeName": "Ana Perez",
								  "details": "Acceso general",
								  "unitPrice": 15.00,
								  "quantity": 1
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$[0].status").value("GENERATED"));

		mockMvc.perform(get("/api/tickets").param("eventId", "event-api"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].eventId").value("event-api"));
	}
}
