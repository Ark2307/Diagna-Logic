package com.meetingiq.platform.service;

import com.meetingiq.platform.api.dto.GenerateRequestDto;
import com.meetingiq.platform.api.dto.GenerateResponseDto;
import com.meetingiq.platform.api.exception.BadRequestException;
import com.meetingiq.platform.api.exception.ResourceNotFoundException;
import com.meetingiq.platform.config.RagProperties;
import com.meetingiq.platform.domain.Dialog;
import com.meetingiq.platform.domain.Meeting;
import com.meetingiq.platform.llm.core.LlmProviderRegistry;
import com.meetingiq.platform.llm.spi.LlmOptions;
import com.meetingiq.platform.llm.spi.LlmProvider;
import com.meetingiq.platform.llm.spi.LlmResult;
import com.meetingiq.platform.llm.task.GenerationQuery;
import com.meetingiq.platform.llm.task.GenerationResult;
import com.meetingiq.platform.llm.task.MapSummaryQuery;
import com.meetingiq.platform.llm.task.PromptLibrary;
import com.meetingiq.platform.rag.ChunkCandidate;
import com.meetingiq.platform.rag.ChunkPlanner;
import com.meetingiq.platform.rag.GenerationPlan;
import com.meetingiq.platform.repository.DialogRepository;
import com.meetingiq.platform.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Backs {@code POST /api/v1/ai/generate}. Deliberately NOT RAG-grounded —
 * summarization and minutes need whole-document coverage, whereas retrieval
 * is for answering a specific question ({@code MeetingChatService}).
 * {@link ChunkPlanner} decides whether the meeting fits a single prompt;
 * the handful that don't go through a map (per-section summary) then
 * reduce (combine into the final result) pass, using the same
 * {@link GenerationQuery} for both the single-pass and the reduce call.
 */
@Service
public class TextGenerationService {

    private final MeetingRepository meetingRepository;
    private final DialogRepository dialogRepository;
    private final ChunkPlanner chunkPlanner;
    private final TranscriptFormatter transcriptFormatter;
    private final LlmProviderRegistry providerRegistry;
    private final RagProperties ragProperties;

    public TextGenerationService(
            MeetingRepository meetingRepository,
            DialogRepository dialogRepository,
            ChunkPlanner chunkPlanner,
            TranscriptFormatter transcriptFormatter,
            LlmProviderRegistry providerRegistry,
            RagProperties ragProperties
    ) {
        this.meetingRepository = meetingRepository;
        this.dialogRepository = dialogRepository;
        this.chunkPlanner = chunkPlanner;
        this.transcriptFormatter = transcriptFormatter;
        this.providerRegistry = providerRegistry;
        this.ragProperties = ragProperties;
    }

    public GenerateResponseDto generate(GenerateRequestDto request) {
        String meetingId = resolveMeetingId(request);
        Meeting meeting = meetingRepository.findFullById(meetingId)
                .orElseThrow(() -> ResourceNotFoundException.meeting(meetingId));

        LlmProvider provider = providerRegistry.resolveLlm(request.provider());
        LlmOptions jsonOptions = request.model() != null && !request.model().isBlank()
                ? LlmOptions.jsonDefaults().withModel(request.model())
                : LlmOptions.jsonDefaults();
        String instructions = effectiveInstructions(request);

        GenerationPlan plan = chunkPlanner.plan(meeting.estimatedTokens(), meeting.transcriptSegments(), ragProperties.generationChunkBudgetTokens());

        LlmResult<GenerationResult> result = plan.singlePass()
                ? runSinglePass(provider, request.task(), instructions, meeting, jsonOptions)
                : runMapReduce(provider, request.task(), instructions, meetingId, plan.mapChunks(), jsonOptions);

        return new GenerateResponseDto(
                result.payload().text(), result.payload().structured(),
                result.providerId(), result.model(), result.usage(), result.latency().toMillis(), result.cached()
        );
    }

    private LlmResult<GenerationResult> runSinglePass(
            LlmProvider provider, com.meetingiq.platform.domain.enums.GenerationTask task, String instructions, Meeting meeting, LlmOptions options
    ) {
        String contextText = transcriptFormatter.format(meeting.transcriptSegments());
        GenerationQuery query = new GenerationQuery(task, instructions, contextText, meeting.id(), options);
        return provider.execute(query);
    }

    private LlmResult<GenerationResult> runMapReduce(
            LlmProvider provider, com.meetingiq.platform.domain.enums.GenerationTask task, String instructions,
            String meetingId, List<ChunkCandidate> mapChunks, LlmOptions options
    ) {
        LlmOptions mapOptions = new LlmOptions(options.model(), 0.3, 800, false);
        List<String> partialSummaries = new ArrayList<>(mapChunks.size());
        for (ChunkCandidate chunk : mapChunks) {
            MapSummaryQuery mapQuery = new MapSummaryQuery(chunk.text(), meetingId, mapOptions);
            partialSummaries.add(provider.execute(mapQuery).payload());
        }
        String reduceContext = PromptLibrary.reduceUserPrompt(partialSummaries);
        GenerationQuery reduceQuery = new GenerationQuery(task, instructions, reduceContext, meetingId, options);
        return provider.execute(reduceQuery);
    }

    private String resolveMeetingId(GenerateRequestDto request) {
        if (request.meetingId() != null && !request.meetingId().isBlank()) {
            return request.meetingId();
        }
        if (request.dialogId() != null && !request.dialogId().isBlank()) {
            Dialog dialog = dialogRepository.findById(request.dialogId())
                    .orElseThrow(() -> ResourceNotFoundException.dialog(request.dialogId()));
            return dialog.meetingId();
        }
        throw new BadRequestException("Either meetingId or dialogId must be provided");
    }

    private static String effectiveInstructions(GenerateRequestDto request) {
        StringBuilder sb = new StringBuilder();
        if (request.instructions() != null && !request.instructions().isBlank()) {
            sb.append(request.instructions());
        }
        if (request.maxWords() != null && request.maxWords() > 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("Keep the total response to approximately ").append(request.maxWords()).append(" words.");
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
