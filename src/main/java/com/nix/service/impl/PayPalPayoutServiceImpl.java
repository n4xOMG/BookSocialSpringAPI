package com.nix.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.nix.service.PayPalPayoutService;

/**
 * Simple PayPal payout service using REST endpoints. This is intentionally
 * minimal. It creates a payout batch and returns the batch id. You should
 * exchange client credentials for an access token and call Payouts API.
 */
@Service
public class PayPalPayoutServiceImpl implements PayPalPayoutService {
	private static final Logger log = LoggerFactory.getLogger(PayPalPayoutServiceImpl.class);

	@Value("${paypal.clientId:}")
	private String clientId;

	@Value("${paypal.clientSecret:}")
	private String clientSecret;

	@Value("${paypal.baseUrl:https://api-m.sandbox.paypal.com}")
	private String baseUrl;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public String createPayout(String paypalEmail, BigDecimal amount, String currency, String note) throws Exception {
		if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
			throw new IllegalStateException("PayPal credentials not configured");
		}
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Payout amount must be greater than zero");
		}

		String accessToken = obtainAccessToken();
		BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
		String normalizedCurrency = currency == null ? "USD" : currency.toUpperCase();

		// Build minimal Payouts request payload
		String senderBatchId = UUID.randomUUID().toString();
		String payload = "{\n" + "  \"sender_batch_header\": {\n" + "    \"sender_batch_id\": \"" + senderBatchId
				+ "\",\n" + "    \"email_subject\": \"You have a payout\"\n" + "  },\n" + "  \"items\": [\n" + "    {\n"
				+ "      \"recipient_type\": \"EMAIL\",\n" + "      \"amount\": {\n" + "        \"value\": \""
				+ normalizedAmount.toPlainString() + "\",\n" + "        \"currency\": \"" + normalizedCurrency + "\"\n"
				+ "      },\n"
				+ "      \"receiver\": \"" + paypalEmail + "\",\n" + "      \"note\": \""
				+ (note == null ? "" : note.replace("\"", "'")) + "\"\n" + "    }\n" + "  ]\n" + "}";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(accessToken);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = new HttpEntity<>(payload, headers);

		try {
			String apiBase = normalizeBaseUrl(baseUrl);
			var response = restTemplate.postForEntity(apiBase + "/v1/payments/payouts", entity,
					PayPalPayoutResponse.class);
			PayPalPayoutResponse payoutResponse = response.getBody();
			if (payoutResponse != null && payoutResponse.batch_header != null
					&& payoutResponse.batch_header.payout_batch_id != null) {
				return payoutResponse.batch_header.payout_batch_id; // Return the actual PayPal batch ID
			}
			throw new Exception("Failed to extract payout_batch_id from PayPal response");
		} catch (RestClientException ex) {
			throw new Exception("PayPal payout request failed: " + ex.getMessage(), ex);
		}
	}

	@Override
	public PayPalPayoutStatus getPayoutBatchStatus(String payoutBatchId) throws Exception {
		if (payoutBatchId == null || payoutBatchId.isBlank()) {
			return PayPalPayoutStatus.UNKNOWN;
		}

		String accessToken = obtainAccessToken();
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

		String apiBase = normalizeBaseUrl(baseUrl);
		String url = apiBase + "/v1/payments/payouts/" + payoutBatchId;
		try {
			HttpEntity<Void> entity = new HttpEntity<>(headers);
			ResponseEntity<PayPalPayoutStatusResponse> resp = restTemplate.exchange(url, HttpMethod.GET, entity,
					PayPalPayoutStatusResponse.class);
			PayPalPayoutStatusResponse statusResponse = resp.getBody();
			if (statusResponse == null || statusResponse.batch_header == null) {
				return PayPalPayoutStatus.UNKNOWN;
			}

			String batchStatus = statusResponse.batch_header.batch_status;
			if (batchStatus == null) {
				return PayPalPayoutStatus.UNKNOWN;
			}

			switch (batchStatus.toUpperCase()) {
				case "SUCCESS":
					return PayPalPayoutStatus.SUCCESS;
				case "PENDING":
					return PayPalPayoutStatus.PENDING;
				case "PROCESSING":
					return PayPalPayoutStatus.PROCESSING;
				case "DENIED":
				case "FAILED":
				case "CANCELED":
					return PayPalPayoutStatus.FAILED;
				default:
					return PayPalPayoutStatus.UNKNOWN;
			}
		} catch (RestClientException ex) {
			throw new Exception("PayPal payout status request failed: " + ex.getMessage(), ex);
		}
	}

	private String obtainAccessToken() throws Exception {
		String cid = safe(clientId);
		String csec = safe(clientSecret);
		if (cid == null || cid.isBlank() || csec == null || csec.isBlank()) {
			throw new IllegalStateException("PayPal credentials not configured");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		headers.setBasicAuth(cid, csec, StandardCharsets.UTF_8);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "client_credentials");

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

		try {
			String apiBase = normalizeBaseUrl(baseUrl);
			String url = apiBase + "/v1/oauth2/token";
			if (log.isDebugEnabled()) {
				log.debug("Requesting PayPal token at {} (clientId length={})", url, cid.length());
			}
			ResponseEntity<PayPalAccessTokenResponse> resp = restTemplate.postForEntity(url, entity,
					PayPalAccessTokenResponse.class);
			if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && resp.getBody().access_token != null
					&& !resp.getBody().access_token.isBlank()) {
				return resp.getBody().access_token;
			}
			throw new Exception("Unable to parse PayPal access token response, status=" + resp.getStatusCode());
		} catch (RestClientException ex) {
			throw new Exception("PayPal token request failed: " + ex.getMessage(), ex);
		}
	}

	private static String safe(String s) {
		return s == null ? null : s.trim();
	}

	private static String normalizeBaseUrl(String url) {
		if (url == null)
			return "https://api-m.sandbox.paypal.com";
		String trimmed = url.trim();
		// remove trailing slashes
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	// Minimal mapping classes for JSON parsing
	@SuppressWarnings("unused")
	private static class PayPalAccessTokenResponse {
		public String access_token;
	}

	@SuppressWarnings("unused")
	private static class PayPalPayoutResponse {
		public PayPalBatchHeader batch_header;

		public static class PayPalBatchHeader {
			public String payout_batch_id;
			public String batch_status;
		}
	}

	@SuppressWarnings("unused")
	private static class PayPalPayoutStatusResponse {
		public PayPalBatchHeader batch_header;

		public static class PayPalBatchHeader {
			public String payout_batch_id;
			public String batch_status;
			public String time_created;
			public String time_completed;
			public String time_closed;
		}
	}
}
