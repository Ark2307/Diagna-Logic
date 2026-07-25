package com.diagna.logic.api;

import com.diagna.logic.api.dto.ChatRequestDto;
import com.diagna.logic.api.dto.ChatResponseDto;
import com.diagna.logic.api.dto.GenerateRequestDto;
import com.diagna.logic.api.dto.GenerateResponseDto;
import com.diagna.logic.domain.ChatConversation;
import com.diagna.logic.service.MeetingChatService;
import com.diagna.logic.service.TextGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The AI-backed endpoints: RAG chat (hard-scoped to one meeting, never
 * answers out of scope — see {@code ScopeGuard}), and meeting-context text
 * generation (summaries, minutes, decisions, action items, topics).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final MeetingChatService chatService;
    private final TextGenerationService generationService;

    public AiController(MeetingChatService chatService, TextGenerationService generationService) {
        this.chatService = chatService;
        this.generationService = generationService;
    }

    @PostMapping("/chat")
    public ChatResponseDto chat(@RequestBody @Valid ChatRequestDto request) {
        return chatService.ask(request);
    }

    @PostMapping("/generate")
    public GenerateResponseDto generate(@RequestBody @Valid GenerateRequestDto request) {
        return generationService.generate(request);
    }

    @GetMapping("/meetings/{id}/conversations")
    public List<ChatConversation> listConversations(@PathVariable String id) {
        return chatService.listConversations(id);
    }

    @GetMapping("/conversations/{id}")
    public ChatConversation getConversation(@PathVariable String id) {
        return chatService.getConversation(id);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String id) {
        chatService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
