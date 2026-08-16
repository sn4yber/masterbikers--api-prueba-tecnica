package com.masterbikers.master_bikers.extraction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ExtractionRequestHasher {

	public String hash(ExtractionCreateRequest request) {
		String canonicalIds = request.productIds().stream()
				.sorted()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(canonicalIds.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
