package com.diagna.logic.llm.spi;

/**
 * A model-agnostic request: everything about WHAT is being asked (task name,
 * prompts, how to parse the answer) and nothing about HOW a particular
 * vendor's SDK is called. Every concrete task in this app (summarize a
 * meeting, answer a grounded question, generate meeting minutes) is a
 * subclass of this — see {@code com.diagna.logic.llm.task}.
 *
 * <p>{@code com.diagna.logic.llm.core.AbstractLlmProvider#execute} is the one
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
     * The meeting or dialog id this query is about, if any — folded into the
     * cache key and recorded on the {@code llm_invocations} audit trail so
     * invocations can be filtered per meeting. {@code null} when not
     * applicable (e.g. a query with no single subject).
     */
    public String targetId() {
        return null;
    }
}
