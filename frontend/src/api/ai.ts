import { apiDelete, apiGet, apiPost } from "./client";
import type { ChatConversation, ChatRequest, ChatResponse, GenerateRequest, GenerateResponse } from "./types";

export function sendChatMessage(request: ChatRequest): Promise<ChatResponse> {
    return apiPost("/ai/chat", request);
}

export function generateText(request: GenerateRequest): Promise<GenerateResponse> {
    return apiPost("/ai/generate", request);
}

export function listConversations(meetingId: string): Promise<ChatConversation[]> {
    return apiGet(`/ai/meetings/${meetingId}/conversations`);
}

export function getConversation(conversationId: string): Promise<ChatConversation> {
    return apiGet(`/ai/conversations/${conversationId}`);
}

export function deleteConversation(conversationId: string): Promise<void> {
    return apiDelete(`/ai/conversations/${conversationId}`);
}
