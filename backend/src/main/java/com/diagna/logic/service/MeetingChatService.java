package com.diagna.logic.service;

import com.diagna.logic.api.dto.ChatRequestDto;
import com.diagna.logic.api.dto.ChatResponseDto;
import com.diagna.logic.api.dto.RetrievalInfoDto;
import com.diagna.logic.api.exception.BadRequestException;
import com.diagna.logic.api.exception.ResourceNotFoundException;
import com.diagna.logic.config.ChatProperties;
import com.diagna.logic.config.RagProperties;
import com.diagna.logic.domain.ChatConversation;
import com.diagna.logic.domain.ChatMessage;
import com.diagna.logic.domain.ChatRole;
import com.diagna.logic.domain.Meeting;
import com.diagna.logic.domain.MeetingChunk;
import com.diagna.logic.domain.TokenUsage;
import com.diagna.logic.llm.core.LlmProviderRegistry;
import com.diagna.logic.llm.spi.LlmOptions;
import com.diagna.logic.llm.spi.LlmProvider;
import com.diagna.logic.llm.spi.LlmResult;
import com.diagna.logic.llm.task.GroundedAnswerQuery;
import com.diagna.logic.rag.ConversationContextBuilder;
import com.diagna.logic.rag.ContextPacker;
import com.diagna.logic.rag.GroundedAnswer;
import com.diagna.logic.rag.GuardedAnswer;
import com.diagna.logic.rag.MeetingRetriever;
import com.diagna.logic.rag.PackedContext;
import com.diagna.logic.rag.RetrievalResult;
import com.diagna.logic.rag.ScopeGuard;
import com.diagna.logic.rag.ScoredChunk;
import com.diagna.logic.repository.ChatConversationRepository;
import com.diagna.logic.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Orchestrates one RAG chat turn end to end: retrieve -> scope-guard the
 * relevance floor -> (if in scope) pack context, call the LLM, verify
 * citations -> persist both the user and assistant messages. See
 * {@code ScopeGuard} for the four-layer enforcement this method drives
 * layers 1, 2 and 4 of (layer 3 is the prompt contract inside
 * {@link GroundedAnswerQuery}).
 */
@Service
public class MeetingChatService {

    private final MeetingRepository meetingRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final MeetingRetriever retriever;
    private final ScopeGuard scopeGuard;
    private final ContextPacker contextPacker;
    private final ConversationContextBuilder conversationContextBuilder;
    private final LlmProviderRegistry providerRegistry;
    private final RagProperties ragProperties;
    private final ChatProperties chatProperties;

    public MeetingChatService(
            MeetingRepository meetingRepository,
            ChatConversationRepository chatConversationRepository,
            MeetingRetriever retriever,
            ScopeGuard scopeGuard,
            ContextPacker contextPacker,
            ConversationContextBuilder conversationContextBuilder,
            LlmProviderRegistry providerRegistry,
            RagProperties ragProperties,
            ChatProperties chatProperties
    ) {
        this.meetingRepository = meetingRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.retriever = retriever;
        this.scopeGuard = scopeGuard;
        this.contextPacker = contextPacker;
        this.conversationContextBuilder = conversationContextBuilder;
        this.providerRegistry = providerRegistry;
        this.ragProperties = ragProperties;
        this.chatProperties = chatProperties;
    }

    public ChatResponseDto ask(ChatRequestDto request) {
        String meetingId = request.meetingId();
        Meeting meeting = meetingRepository.findFullById(meetingId)
                .orElseThrow(() -> ResourceNotFoundException.meeting(meetingId));
        ChatConversation conversation = request.conversationId() != null && !request.conversationId().isBlank()
                ? loadConversationScopedTo(request.conversationId(), meetingId)
                : newConversation(meetingId, request.message());

        String retrievalQuery = conversationContextBuilder.rewriteForRetrieval(request.message(), conversation.messages());
        RetrievalResult retrieval = retriever.retrieve(meetingId, retrievalQuery, ragProperties.topK(), request.provider());

        Optional<GuardedAnswer> outOfScope = scopeGuard.checkRelevanceFloor(retrieval.topCosineScore());

        GuardedAnswer guarded;
        String usedProvider = null;
        String usedModel = null;
        TokenUsage usage = TokenUsage.ZERO;
        long latencyMs = 0;
        List<String> retrievedChunkIds = List.of();
        boolean usedFullTranscript = false;

        if (outOfScope.isPresent()) {
            // Layer 2 short-circuit: the LLM is never called, so there is genuinely no
            // provider/model/usage to report — zero tokens spent, by construction.
            guarded = outOfScope.get();
        } else {
            usedFullTranscript = meeting.estimatedTokens() <= ragProperties.maxFullTranscriptTokens()
                    && ragProperties.contextMode() != RagProperties.ContextMode.RETRIEVAL_ONLY;

            PackedContext context = usedFullTranscript
                    ? contextPacker.packFullTranscript(meeting.transcriptSegments())
                    : contextPacker.packChunks(
                            retrieval.topChunks().stream().map(ScoredChunk::chunk).toList(),
                            ragProperties.maxFullTranscriptTokens());

            Set<Integer> allowedIndices = usedFullTranscript
                    ? IntStream.range(0, meeting.segmentCount()).boxed().collect(Collectors.toSet())
                    : context.includedChunks().stream()
                            .flatMap(c -> IntStream.rangeClosed(c.startIndex(), c.endIndex()).boxed())
                            .collect(Collectors.toSet());

            String historyText = conversationContextBuilder.buildHistoryText(conversation.messages(), chatProperties.historyBudgetTokens());

            LlmProvider provider = providerRegistry.resolveLlm(request.provider());
            LlmOptions options = request.model() != null && !request.model().isBlank()
                    ? LlmOptions.jsonDefaults().withModel(request.model())
                    : LlmOptions.jsonDefaults();

            GroundedAnswerQuery query = new GroundedAnswerQuery(meetingId, historyText, context.text(), request.message(), options);
            LlmResult<GroundedAnswer> result = provider.execute(query);

            guarded = scopeGuard.verify(result.payload(), allowedIndices, meeting.transcriptSegments(), meeting.segmentCount());
            usedProvider = result.providerId();
            usedModel = result.model();
            usage = result.usage();
            latencyMs = result.latency().toMillis();
            retrievedChunkIds = context.includedChunks().stream().map(MeetingChunk::id).toList();
        }

        ChatConversation saved = persistTurn(conversation, request.message(), guarded, retrievedChunkIds, usedProvider, usedModel, usage, latencyMs);

        return new ChatResponseDto(
                saved.id(), guarded.answer(), guarded.unanswerable(),
                guarded.reason() == null ? null : guarded.reason().name(), guarded.citations(),
                new RetrievalInfoDto(retrievedChunkIds, retrieval.topCosineScore(), usedFullTranscript),
                usedProvider, usedModel, usage, latencyMs
        );
    }

    public List<ChatConversation> listConversations(String meetingId) {
        if (!meetingRepository.existsById(meetingId)) {
            throw ResourceNotFoundException.meeting(meetingId);
        }
        return chatConversationRepository.findByMeetingIdOrderByUpdatedAtDesc(meetingId);
    }

    public ChatConversation getConversation(String conversationId) {
        return chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("No conversation found with id '" + conversationId + "'"));
    }

    public void deleteConversation(String conversationId) {
        chatConversationRepository.deleteById(conversationId);
    }

    private ChatConversation loadConversationScopedTo(String conversationId, String meetingId) {
        ChatConversation conversation = getConversation(conversationId);
        if (!conversation.meetingId().equals(meetingId)) {
            // Layer 1 of ScopeGuard: a conversation's meeting is immutable — a follow-up can never
            // redirect an existing thread to answer questions about a different meeting.
            throw new BadRequestException(
                    "Conversation '" + conversationId + "' belongs to meeting '" + conversation.meetingId() + "', not '" + meetingId + "'");
        }
        return conversation;
    }

    private ChatConversation newConversation(String meetingId, String firstMessage) {
        String title = firstMessage.length() > 80 ? firstMessage.substring(0, 80) + "..." : firstMessage;
        Instant now = Instant.now();
        return new ChatConversation(UUID.randomUUID().toString(), meetingId, title, now, now, List.of());
    }

    private ChatConversation persistTurn(
            ChatConversation conversation, String userMessage, GuardedAnswer guarded,
            List<String> retrievedChunkIds, String provider, String model, TokenUsage usage, long latencyMs
    ) {
        int userIndex = conversation.messages().size();
        Instant now = Instant.now();
        ChatMessage userMsg = ChatMessage.user(userIndex, userMessage, now);
        ChatMessage assistantMsg = new ChatMessage(
                userIndex + 1, ChatRole.ASSISTANT, guarded.answer(), guarded.citations(),
                guarded.unanswerable(), guarded.reason(), retrievedChunkIds,
                provider, model, usage, latencyMs, now
        );
        ChatConversation withUser = conversation.withMessageAppended(userMsg, now);
        ChatConversation withBoth = withUser.withMessageAppended(assistantMsg, now);
        return chatConversationRepository.save(withBoth);
    }
}
