package com.duocuc.sistema_tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

	@Id
	@Column(length = 36)
	private String id;

	@Column(nullable = false, length = 80)
	private String eventId;

	@Column(nullable = false, length = 160)
	private String eventName;

	@Column(nullable = false, length = 80)
	private String userId;

	@Column(nullable = false, length = 160)
	private String attendeeName;

	@Column(nullable = false, length = 1000)
	private String details;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketStatus status;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(length = 240)
	private String s3Key;

	@Column(length = 1024)
	private String s3VersionId;

	protected Ticket() {
	}

	public Ticket(String eventId, String eventName, String userId, String attendeeName,
			String details, BigDecimal unitPrice) {
		this.id = UUID.randomUUID().toString();
		this.eventId = eventId;
		this.eventName = eventName;
		this.userId = userId;
		this.attendeeName = attendeeName;
		this.details = details;
		this.unitPrice = unitPrice;
		this.status = TicketStatus.GENERATED;
		this.createdAt = Instant.now();
		this.s3Key = eventId + "/" + userId + "/" + id + ".pdf";
	}

	public void updateDetails(String attendeeName, String details) {
		this.attendeeName = attendeeName;
		this.details = details;
	}

	public void markStored(String versionId) {
		this.status = TicketStatus.STORED;
		this.s3VersionId = versionId;
	}

	public String getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public String getEventName() {
		return eventName;
	}

	public String getUserId() {
		return userId;
	}

	public String getAttendeeName() {
		return attendeeName;
	}

	public String getDetails() {
		return details;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public TicketStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getS3Key() {
		return s3Key;
	}

	public String getS3VersionId() {
		return s3VersionId;
	}
}
