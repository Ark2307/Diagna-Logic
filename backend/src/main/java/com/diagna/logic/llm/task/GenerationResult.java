package com.diagna.logic.llm.task;

/** The JSON payload a {@link GenerationQuery} parses its response into — the {@code /ai/generate} response body's core. */
public record GenerationResult(String text, GenerationStructured structured) {
}
