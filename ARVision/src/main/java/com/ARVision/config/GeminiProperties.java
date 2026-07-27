package com.ARVision.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini chatbot configuration. Bound from {@code gemini.api.*} in application.properties.
 * <p>
 * Example:
 * <pre>
 * gemini.api.key=AIza...
 * gemini.api.model=gemini-1.5-flash
 * gemini.api.url=https://generativelanguage.googleapis.com/v1beta
 * gemini.api.timeout-ms=15000
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiProperties {

    /** API key. If blank, the chatbot falls back to the rule-based router. */
    private String key;

    /** Model name, e.g. {@code gemini-1.5-flash} (cheap+fast) or {@code gemini-1.5-pro}. */
    private String model = "gemini-1.5-flash";

    /** Base URL — change to point at a proxy if needed. */
    private String url = "https://generativelanguage.googleapis.com/v1beta";

    /** Max Gemini call duration before the chatbot falls back to rules. */
    private long timeoutMs = 15_000;
}