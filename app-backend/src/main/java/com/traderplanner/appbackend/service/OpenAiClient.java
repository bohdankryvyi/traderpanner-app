package com.traderplanner.appbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
	private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Send chat completion request with JSON mode. Returns the content of the first
	 * choice. Does not log the API key.
	 * 
	 * @param model
	 *            e.g. gpt-4o-mini
	 */
	public String chat(List<Map<String, String>> messages, String apiKey, String model) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalArgumentException("OpenAI API key is required");
		}
		String effectiveModel = (model != null && !model.isBlank()) ? model : "gpt-4o-mini";
		try {
			Map<String, Object> body = Map.of("model", effectiveModel, "messages", messages, "response_format",
					Map.of("type", "json_object"));
			String bodyJson = objectMapper.writeValueAsString(body);
			if (log.isDebugEnabled()) {
				log.debug("OpenAI request model={} messages={}", effectiveModel, messages.size());
			}
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(CHAT_URL)).timeout(REQUEST_TIMEOUT)
					.header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey.trim())
					.POST(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (log.isDebugEnabled()) {
				log.debug("OpenAI response status={}", response.statusCode());
			}
			if (response.statusCode() != 200) {
				throw new RuntimeException("OpenAI API returned " + response.statusCode() + ": " + response.body());
			}
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode choices = root.get("choices");
			if (choices == null || !choices.isArray() || choices.isEmpty()) {
				throw new RuntimeException("OpenAI response has no choices");
			}
			String content = choices.get(0).path("message").path("content").asText(null);
			if (content == null || content.isBlank()) {
				throw new RuntimeException("OpenAI response content is empty");
			}
			if (log.isDebugEnabled()) {
				log.debug("OpenAI response content length={}", content.length());
			}
			return content;
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("OpenAI request failed: {}", e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}
}
