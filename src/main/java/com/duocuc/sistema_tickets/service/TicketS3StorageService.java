package com.duocuc.sistema_tickets.service;

import com.duocuc.sistema_tickets.controller.TicketVersionResponse;
import com.duocuc.sistema_tickets.model.Ticket;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

@Service
public class TicketS3StorageService {

	private final S3Client s3Client;
	private final String bucket;

	public TicketS3StorageService(S3Client s3Client, @Value("${app.aws.s3.bucket:}") String bucket) {
		this.s3Client = s3Client;
		this.bucket = bucket.trim();
	}

	public String upload(Ticket ticket, Path file) {
		requireBucket();
		return s3Client.putObject(
				PutObjectRequest.builder()
						.bucket(bucket)
						.key(ticket.getS3Key())
						.contentType("application/pdf")
						.build(),
				RequestBody.fromFile(file)).versionId();
	}

	public byte[] download(Ticket ticket) {
		requireBucket();
		ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
				GetObjectRequest.builder().bucket(bucket).key(ticket.getS3Key()).build());
		return response.asByteArray();
	}

	public void delete(Ticket ticket) {
		requireBucket();
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(ticket.getS3Key()).build());
	}

	public List<TicketVersionResponse> versions(Ticket ticket) {
		requireBucket();
		List<TicketVersionResponse> result = new ArrayList<>();
		var versions = s3Client.listObjectVersionsPaginator(ListObjectVersionsRequest.builder()
						.bucket(bucket)
						.prefix(ticket.getS3Key())
						.build());
		versions.versions().stream()
				.filter(version -> ticket.getS3Key().equals(version.key()))
				.map(this::toVersionResponse)
				.forEach(result::add);
		versions.deleteMarkers().stream()
				.filter(marker -> ticket.getS3Key().equals(marker.key()))
				.map(this::toDeleteMarkerResponse)
				.forEach(result::add);
		return result;
	}

	private TicketVersionResponse toVersionResponse(ObjectVersion version) {
		Instant modified = version.lastModified() == null ? null : version.lastModified();
		return new TicketVersionResponse(version.versionId(), modified, Boolean.TRUE.equals(version.isLatest()), false);
	}

	private TicketVersionResponse toDeleteMarkerResponse(DeleteMarkerEntry marker) {
		Instant modified = marker.lastModified() == null ? null : marker.lastModified();
		return new TicketVersionResponse(marker.versionId(), modified, Boolean.TRUE.equals(marker.isLatest()), true);
	}

	private void requireBucket() {
		if (bucket.isBlank()) {
			throw new S3StorageNotConfiguredException();
		}
	}
}
