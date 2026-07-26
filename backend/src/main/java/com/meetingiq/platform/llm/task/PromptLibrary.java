package com.meetingiq.platform.llm.task;

import com.meetingiq.platform.domain.enums.GenerationTask;

import java.util.List;

/**
 * The system prompts for every task query in this app. Kept in one place
 * so the prompt contract each task relies on (especially the grounded-chat
 * citation rules {@code ScopeGuard} verifies against) is easy to find and
 * change without hunting through query classes.
 */
public final class PromptLibrary {

    private PromptLibrary() {
    }

    /**
     * The grounding contract {@link GroundedAnswerQuery} gives every
     * provider — this is what makes {@code ScopeGuard}'s citation
     * verification meaningful: the model is told exactly what shape to
     * answer in and exactly what "cite your source" means.
     */
    public static String groundedChatSystemPrompt() {
        return """
                You are answering questions about ONE specific meeting transcript. You will be given \
                numbered transcript passages (format: "[index] Speaker: text") and a question, possibly \
                with prior conversation turns for context.

                Rules:
                - Answer ONLY using the supplied passages. Do not use outside knowledge or assumptions.
                - Every factual claim in your answer must be supported by at least one passage you were shown.
                - Respond with STRICT JSON only, no other text, in exactly this shape:
                  {"answer": "<your answer>", "unanswerable": <true|false>, "citedSegmentIndices": [<int>, ...]}
                - Write the "answer" value as Markdown (bold, bullet/numbered lists, short headings) wherever \
                it improves readability — it is rendered with a Markdown viewer on the client. Do not put \
                segment brackets like "[12]" inside the answer text itself; citations belong only in \
                citedSegmentIndices.
                - citedSegmentIndices must be the numeric indices (the numbers in brackets) of the passages \
                that support your answer. Include every index you relied on.
                - If the supplied passages do not contain an answer to the question, set "unanswerable": true, \
                set "answer" to a brief note saying so, and set "citedSegmentIndices" to an empty array.
                - Never invent a segment index that was not shown to you.""";
    }

    /** The task-specific instruction folded into {@link #generationSystemPrompt}. */
    private static String taskInstruction(GenerationTask task) {
        return switch (task) {
            case SUMMARY -> "Write a concise prose overview of this meeting.";
            case MINUTES -> "Write formal meeting minutes: overview, key discussion points, decisions, and action items.";
            case DECISIONS -> "Extract only the decisions reached during this meeting, as a list.";
            case ACTION_ITEMS -> "Extract actionable follow-ups from this meeting. Include an owner if the transcript names one.";
            case TOPICS -> "List the main topics discussed in this meeting.";
            case CUSTOM -> "Follow the additional instructions provided about this meeting.";
        };
    }

    public static String generationSystemPrompt(GenerationTask task) {
        return """
                You are analyzing a meeting transcript (numbered "[index] Speaker: text" lines, or a set \
                of section summaries if the transcript was processed in sections because of its length).

                Task: %s

                Respond with STRICT JSON only, no other text, in exactly this shape:
                {"text": "<main textual output for the task>",
                 "structured": {"overview": "<short overview>", "keyPoints": ["..."], "decisions": ["..."],
                                 "actionItems": ["..."], "topics": ["..."], "participants": ["..."]}}
                Write "text" (and each structured string) as Markdown (bold, bullet/numbered lists, short \
                headings) wherever it improves readability — these values are rendered with a Markdown viewer \
                on the client, not displayed as plain text.
                Leave a structured field as an empty list or empty string if the task or the transcript has \
                nothing to report for it — never fabricate content to fill it. Always include the top-level \
                "structured" object and all six of its fields — never omit "structured" itself or any of its \
                keys, even when several are empty.""".formatted(taskInstruction(task));
    }

    /** Plain-prose (not JSON) system prompt for one map-step partial summary — see {@code ChunkPlanner}. */
    public static String mapSummarySystemPrompt() {
        return """
                Summarize the following meeting transcript excerpt concisely, preserving key facts, names, \
                decisions and action items mentioned in it. Plain prose only — no JSON, no headers, no bullet points.""";
    }

    /** Builds the reduce-step user prompt from the map step's ordered partial summaries. */
    public static String reduceUserPrompt(List<String> partialSummaries) {
        StringBuilder sb = new StringBuilder("The following are summaries of consecutive sections of one meeting, in order:\n\n");
        for (int i = 0; i < partialSummaries.size(); i++) {
            sb.append("Section ").append(i + 1).append(":\n").append(partialSummaries.get(i)).append("\n\n");
        }
        return sb.toString();
    }
}
