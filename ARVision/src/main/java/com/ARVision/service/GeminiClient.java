package com.ARVision.service;

import com.ARVision.config.GeminiProperties;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Thin wrapper around the Google Gemini generateContent REST endpoint.
 * <p>
 * Uses Spring 6 {@link RestClient} (no extra dependency — already on the
 * classpath via spring-boot-starter-web) and Gson (already in pom.xml).
 * <p>
 * If the API key is missing or the call fails for any reason, callers get
 * an empty {@link Optional} so the chatbot service can fall back to its
 * rule-based router instead of crashing the response.
 */
@Slf4j
@Service
public class GeminiClient {

    private final GeminiProperties props;
    private final RestClient http;
    private final Gson gson = new Gson();

    public GeminiClient(GeminiProperties props) {
        this.props = props;
        this.http = RestClient.builder()
                .baseUrl(props.getUrl())
                .build();
    }

    /**
     * Sends a single user message to Gemini and returns the model's text reply.
     *
     * @param userMessage the user's question
     * @param systemPrompt optional system instruction (sets persona/constraints)
     * @return the assistant text, or empty if Gemini is unavailable / misconfigured
     */
    public Optional<String> generate(String userMessage, String systemPrompt) {
        if (props.getKey() == null || props.getKey().isBlank()) {
            log.debug("Gemini API key not configured — skipping LLM call.");
            return Optional.empty();
        }
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        JsonObject body = new JsonObject();

        // System instruction (newer Gemini API accepts it at top level).
        // Shape:  "systemInstruction": { "parts": [ { "text": "..." } ] }
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonArray sysParts = new JsonArray();
            JsonObject sysPart = new JsonObject();
            sysPart.addProperty("text", systemPrompt);
            sysParts.add(sysPart);
            JsonObject sysWrap = new JsonObject();
            sysWrap.add("parts", sysParts);
            body.add("systemInstruction", sysWrap);
        }

        // User turn
        JsonObject userText = new JsonObject();
        userText.addProperty("text", userMessage);
        JsonObject userPart = new JsonObject();
        userPart.add("text", userText);
        JsonArray userParts = new JsonArray();
        userParts.add(userPart);
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", userParts);
        JsonArray contents = new JsonArray();
        contents.add(userContent);
        body.add("contents", contents);

        // Generation config — keep replies concise for chat UI
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.7);
        genConfig.addProperty("maxOutputTokens", 512);
        body.add("generationConfig", genConfig);

        String path = "/models/" + props.getModel() + ":generateContent?key=" + props.getKey();

        try {
            String raw = http.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);

            return Optional.ofNullable(raw)
                    .map(this::extractFirstText)
                    .filter(s -> !s.isBlank());
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            log.warn("Gemini returned non-2xx ({}): {}", status, trim(e.getResponseBodyAsString(), 300));
            return Optional.empty();
        } catch (ResourceAccessException e) {
            log.warn("Gemini call timed out / network error: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.toString());
            return Optional.empty();
        }
    }

    // ── Response parsing ───────────────────────────────────────
    /**
     * Pull the first {@code candidates[0].content.parts[*].text} value out of a
     * Gemini generateContent response. Gson is robust to extra/missing fields.
     */
    private String extractFirstText(String rawJson) {
        try {
            JsonObject root = gson.fromJson(rawJson, JsonObject.class);
            if (root == null) return "";
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return "";
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            if (content == null) return "";
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null) return "";
            StringBuilder sb = new StringBuilder();
            parts.forEach(p -> {
                if (p.isJsonObject() && p.getAsJsonObject().has("text")) {
                    sb.append(p.getAsJsonObject().get("text").getAsString());
                }
            });
            return sb.toString().trim();
        } catch (Exception e) {
            log.debug("Could not parse Gemini response as JSON: {}", e.getMessage());
            return "";
        }
    }

    /** Wraps the system content object in the shape Gemini expects. */
    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Exposed for diagnostics. */
    public Duration timeout() {
        return Duration.ofMillis(props.getTimeoutMs());
    }
}