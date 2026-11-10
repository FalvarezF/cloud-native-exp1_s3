package com.duocuc.sistema_tickets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Configuration {

	@Bean
	S3Client s3Client(@Value("${app.aws.region:us-east-1}") String region) {
		return S3Client.builder().region(Region.of(region)).build();
	}
}
