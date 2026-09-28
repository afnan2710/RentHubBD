package com.aoop.renthubbd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotLlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final boolean debug;

    public ChatbotLlmClient(
            @Value("${chatbot.llm.url}") String url,
            @Value("${chatbot.llm.api-key:}") String apiKey,
            @Value("${chatbot.llm.model}") String model,
            @Value("${chatbot.llm.enabled:true}") boolean enabled,
            @Value("${chatbot.llm.connect-timeout-seconds:10}") int connectTimeout,
            @Value("${chatbot.llm.read-timeout-seconds:30}") int readTimeout,
            @Value("${chatbot.llm.debug:false}") boolean debug,
            ObjectMapper objectMapper) {

        this.model = model;
        this.enabled = enabled;
        this.debug = debug;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(url)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder = builder.defaultHeader("Authorization", "Bearer " + apiKey.trim());
            log("LLM client initialised. URL=" + url + " model=" + model
                    + " key=..." + maskKey(apiKey));
        } else {
            log("WARNING: no api-key set. Outbound calls will likely fail with 401.");
        }

        this.restClient = builder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String complete(List<Map<String, String>> conversation) {
        if (!enabled) {
            log("LLM disabled in properties; skipping outbound call.");
            return null;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, String> turn : conversation) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", turn.get("role"));
            m.put("content", turn.get("content"));
            messages.add(m);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.4);
        body.put("max_tokens", 800);
        body.put("messages", messages);

        try {
            String payload = objectMapper.writeValueAsString(body);
            if (debug) {
                log("Outbound request body (truncated): "
                        + payload.substring(0, Math.min(300, payload.length())) + "...");
            }

            String raw = restClient.post()
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (raw == null) {
                log("Empty response body from LLM.");
                return null;
            }

            if (debug) {
                log("Raw response (truncated): "
                        + raw.substring(0, Math.min(400, raw.length())) + "...");
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                log("No message content in response. Full body: " + raw);
                return null;
            }

            String text = content.asText().trim();

            if (text.startsWith("```")) {
                int firstNewline = text.indexOf('\n');
                int lastFence = text.lastIndexOf("```");
                if (firstNewline > 0 && lastFence > firstNewline) {
                    text = text.substring(firstNewline + 1, lastFence).trim();
                }
            }

            return text;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log("HTTP " + e.getStatusCode() + " from LLM. Body: " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log("Outbound call failed: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            return null;
        }
    }

    private void log(String message) {
        System.out.println("[ChatbotLlmClient] " + message);
    }

    private String maskKey(String key) {
        String k = key.trim();
        if (k.length() <= 8) return "***";
        return k.substring(0, 4) + "..." + k.substring(k.length() - 4);
    }
}