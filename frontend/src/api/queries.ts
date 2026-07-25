import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as ai from "./ai";
import * as dialogs from "./dialogs";
import * as meetings from "./meetings";
import * as searchApi from "./search";
import * as stats from "./stats";
import type { ChatRequest, DialogSearchParams, GenerateRequest, MeetingSearchParams } from "./types";

export function useMeetings(params: MeetingSearchParams) {
    return useQuery({
        queryKey: ["meetings", params],
        queryFn: () => meetings.searchMeetings(params),
        placeholderData: (previous) => previous,
    });
}

export function useMeeting(id: string | undefined, includeTranscript = false) {
    return useQuery({
        queryKey: ["meeting", id, includeTranscript],
        queryFn: () => meetings.getMeeting(id!, includeTranscript),
        enabled: !!id,
    });
}

export function useTranscriptPage(id: string | undefined, from: number, to?: number) {
    return useQuery({
        queryKey: ["meeting-transcript", id, from, to],
        queryFn: () => meetings.getTranscriptPage(id!, from, to),
        enabled: !!id,
        placeholderData: (previous) => previous,
    });
}

export function useMeetingDialogs(id: string | undefined) {
    return useQuery({
        queryKey: ["meeting-dialogs", id],
        queryFn: () => meetings.getMeetingDialogs(id!),
        enabled: !!id,
    });
}

export function useDialogs(params: DialogSearchParams) {
    return useQuery({
        queryKey: ["dialogs", params],
        queryFn: () => dialogs.searchDialogs(params),
        placeholderData: (previous) => previous,
    });
}

export function useDialog(id: string | undefined, resolveAttributions = false) {
    return useQuery({
        queryKey: ["dialog", id, resolveAttributions],
        queryFn: () => dialogs.getDialog(id!, resolveAttributions),
        enabled: !!id,
    });
}

export function useSearch(q: string, scope: "all" | "transcripts" | "dialogs" = "all") {
    return useQuery({
        queryKey: ["search", q, scope],
        queryFn: () => searchApi.search(q, scope),
        enabled: q.trim().length > 0,
    });
}

export function useOverallStats() {
    return useQuery({ queryKey: ["stats"], queryFn: () => stats.getOverallStats() });
}

export function useMeetingStats(meetingId: string | undefined) {
    return useQuery({
        queryKey: ["meeting-stats", meetingId],
        queryFn: () => stats.getMeetingStats(meetingId!),
        enabled: !!meetingId,
    });
}

export function useConversations(meetingId: string | undefined) {
    return useQuery({
        queryKey: ["conversations", meetingId],
        queryFn: () => ai.listConversations(meetingId!),
        enabled: !!meetingId,
    });
}

export function useConversation(conversationId: string | undefined) {
    return useQuery({
        queryKey: ["conversation", conversationId],
        queryFn: () => ai.getConversation(conversationId!),
        enabled: !!conversationId,
    });
}

export function useSendChatMessage() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (request: ChatRequest) => ai.sendChatMessage(request),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: ["conversations", variables.meetingId] });
        },
    });
}

export function useGenerateText() {
    return useMutation({ mutationFn: (request: GenerateRequest) => ai.generateText(request) });
}

export function useDeleteConversation() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (conversationId: string) => ai.deleteConversation(conversationId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["conversations"] });
        },
    });
}
