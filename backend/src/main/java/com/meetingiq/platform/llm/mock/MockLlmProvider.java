package com.meetingiq.platform.llm.mock;

import com.meetingiq.platform.domain.TokenUsage;
import com.meetingiq.platform.llm.core.AbstractLlmProvider;
import com.meetingiq.platform.llm.core.LlmJsonMapper;
import com.meetingiq.platform.llm.core.LlmResponseCache;
import com.meetingiq.platform.llm.spi.FinishReason;
import com.meetingiq.platform.llm.spi.LlmCompletion;
import com.meetingiq.platform.llm.spi.LlmOptions;
import com.meetingiq.platform.llm.spi.LlmQuery;
import com.meetingiq.platform.llm.spi.ProviderDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A deterministic, keyless stand-in for a real chat-completion provider —
 * exists so the whole app (every endpoint, every test, the frontend demo
 * path) runs with zero external dependency and zero cost. It is registered
 * as {@code meetingiq.llm.default-provider}'s default value, precisely so a
 * fresh clone with no {@code OPENAI_API_KEY} works out of the box.
 *
 * <p><strong>What "deterministic" means here:</strong> the response is a
 * pure function of the prompt text — segment indices are read straight out
 * of {@code [<index>]} markers that {@code TranscriptFormatter} puts in
 * every prompt (whether the full transcript or a RAG chunk), so citations
 * in the mock's answer are always real, in-bounds segment references, never
 * fabricated ones. No randomness, no clock.
 *
 * <p><strong>Why one JSON shape covers several tasks:</strong> a
 * task-agnostic mock cannot know the exact payload type a given
 * {@link LlmQuery#parser()} expects (that would mean this package depending
 * on {@code llm.task}, inverting the whole abstraction). Instead this emits
 * one envelope wide enough to satisfy every JSON-producing task shape in
 * this app (grounded answers AND meeting-text generation) at once — paired
 * with {@link LlmJsonMapper}'s lenient deserialization, extra fields a given
 * parser doesn't read are simply ignored rather than a hard error. A real
 * vendor has no such envelope; it returns whatever shape the model itself
 * decided to emit for the prompt it was given.
 */
@Component
public class MockLlmProvider extends AbstractLlmProvider {

    private static final String PROVIDER_ID = "mock";
    private static final String MODEL = "mock-1";
    private static final Pattern SEGMENT_MARKER = Pattern.compile("\\[(\\d+)]");
    private static final int MAX_CITED_SEGMENTS = 3;

    private final ObjectMapper jsonWriter = LlmJsonMapper.create();

    public MockLlmProvider(LlmResponseCache cache) {
        super(cache);
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(PROVIDER_ID, "Mock (offline, deterministic)", true, true, MODEL, MODEL);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    protected String resolveModel(LlmOptions options) {
        String requested = options == null ? null : options.model();
        return (requested != null && !requested.isBlank()) ? requested : MODEL;
    }

    @Override
    protected LlmCompletion doComplete(LlmQuery<?> query, String resolvedModel) {
        List<Integer> citedSegments = extractSegmentIndices(query.userPrompt()).stream()
                .distinct()
                .limit(MAX_CITED_SEGMENTS)
                .toList();
        boolean unanswerable = citedSegments.isEmpty();

        String answer = unanswerable
                ? "This meeting's transcript doesn't appear to cover that."
                : "Based on segment" + (citedSegments.size() > 1 ? "s " : " ")
                        + citedSegments.stream().map(String::valueOf).collect(Collectors.joining(", "))
                        + " of the transcript, here is a mock answer for the '" + query.taskName() + "' task.";

        // Deliberately independent of `unanswerable`: that concept is specific to grounded chat
        // (no cited segment = nothing to answer from), but generation must always produce SOMETHING
        // from whatever content it's given — including ChunkPlanner's map-reduce "reduce" step,
        // whose input is prior partial summaries (real prose, but with no [N] markers to find).
        String generationText = "Mock generated content for task '" + query.taskName() + "', derived from "
                + wordCount(query.userPrompt()) + " words of input"
                + (citedSegments.isEmpty() ? "." : ", referencing segments " + citedSegments + ".");

        String json = toJson(answer, unanswerable, citedSegments, generationText, query.taskName());
        TokenUsage usage = TokenUsage.of(estimateTokens(query.systemPrompt()) + estimateTokens(query.userPrompt()), estimateTokens(json));
        return new LlmCompletion(json, resolvedModel, usage, FinishReason.STOP);
    }

    private String toJson(String answer, boolean unanswerable, List<Integer> citedSegments, String generationText, String taskName) {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("overview", generationText);
        structured.put("keyPoints", citedSegments.stream().map(i -> "Point derived from segment " + i).toList());
        structured.put("decisions", List.of());
        structured.put("actionItems", List.of());
        structured.put("topics", List.of("Mock topic for task " + taskName));
        structured.put("participants", List.of());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("answer", answer);
        envelope.put("unanswerable", unanswerable);
        envelope.put("citedSegmentIndices", citedSegments);
        envelope.put("text", generationText);
        envelope.put("structured", structured);

        try {
            return jsonWriter.writeValueAsString(envelope);
        } catch (Exception e) {
            // Serializing a Map<String, simple-types> cannot realistically fail; if it ever
            // does, that's a bug in this mock, not something a caller should have to handle.
            throw new IllegalStateException("MockLlmProvider failed to serialize its own response", e);
        }
    }

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private static List<Integer> extractSegmentIndices(String text) {
        if (text == null) {
            return List.of();
        }
        List<Integer> indices = new ArrayList<>();
        Matcher matcher = SEGMENT_MARKER.matcher(text);
        while (matcher.find()) {
            indices.add(Integer.parseInt(matcher.group(1)));
        }
        return indices;
    }

    /** Rough token estimate (~4 chars/token) for the mock's own usage reporting — never used for billing. */
    private static int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 4;
    }
}
