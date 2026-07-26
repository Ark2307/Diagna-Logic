package com.meetingiq.platform.llm.spi;

/**
 * A model-agnostic request: everything about WHAT is being asked (task name,
 * prompts, how to parse the answer) and nothing about HOW a particular
 * vendor's SDK is called. Every concrete task in this app (summarize a
 * meeting, answer a grounded question, generate meeting minutes) is a
 * subclass of this — see {@code com.meetingiq.platform.llm.task}.
 *
 * <p>{@code com.meetingiq.platform.llm.core.AbstractLlmProvider#execute} is the one
 * place a query's prompts are turned into a provider call and its parser
 * applied to the response; nothing else in the app should call a provider's
 * SDK directly.
 */
public abstract class LlmQuery<T> {

    private final LlmOptions options;

    protected LlmQuery(LlmOptions options) {
        this.options = options;
    }

    public LlmOptions options() {
        return options;
    }

    /** Short, stable identifier used in caching, logging and the {@code llm_invocations} audit trail. */
    public abstract String taskName();

    public abstract String systemPrompt();

    public abstract String userPrompt();

    /** How to turn the provider's raw text into this query's payload type. */
    public abstract ResponseParser<T> parser();

    /**
     * A JSON Schema for this query's expected response shape, for providers that support
     * enforcing it server-side (e.g. OpenAI's Structured Outputs) instead of best-effort JSON
     * mode. {@code null} when the task has no fixed schema (e.g. a plain-prose query) or
     * best-effort JSON mode is enough — the default. Worth overriding for any task where an
     * incomplete-but-syntactically-valid response is a real risk: a real model asked only in
     * prose for "every field, every time" can still fold structured content into free text and
     * leave the actual structured field null.
     */
    public JsonResponseSchema responseSchema() {
        return null;
    }

    /**
     * The meeting or dialog id this query is about, if any — folded into the
     * cache key and recorded on the {@code llm_invocations} audit trail so
     * invocations can be filtered per meeting. {@code null} when not
     * applicable (e.g. a query with no single subject).
     */
    public String targetId() {
        return null;
    }
}
