package com.meetingiq.platform.llm.spi;

import java.util.Map;

/**
 * A vendor-neutral JSON Schema describing a {@link LlmQuery}'s expected response shape, for
 * providers whose SDK supports enforcing it server-side (e.g. OpenAI's Structured Outputs) rather
 * than merely requesting best-effort JSON. {@code schema} is a plain JSON Schema object (as
 * nested {@code Map}/{@code List}/primitives) — a provider that supports this maps it into
 * whatever type its own SDK requires; a provider that doesn't can ignore it and fall back to
 * loose JSON mode.
 */
public record JsonResponseSchema(String name, Map<String, Object> schema) {
}
