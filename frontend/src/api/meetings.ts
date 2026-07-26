import { apiGet, apiPost } from "./client";
import { buildQueryString } from "./query-string";
import { parseSort } from "./search-request";
import type { Dialog, Meeting, MeetingSearchParams, PageResponse, SpeakerStat, TranscriptPage } from "./types";

export function searchMeetings(params: MeetingSearchParams): Promise<PageResponse<Meeting>> {
    const { q, corpus, domain, split, speaker, minSegments, meetingId, page, size, sort } = params;
    return apiPost("/meetings/search", {
        q: q || undefined,
        filters: { corpus, domain, split, speaker, minSegments, meetingId: meetingId || undefined },
        pagination: { page, size },
        sort: parseSort(sort),
    });
}

export function getMeeting(id: string, includeTranscript = false): Promise<Meeting> {
    return apiGet(`/meetings/${id}${buildQueryString({ includeTranscript })}`);
}

export function getTranscriptPage(id: string, from: number, to?: number): Promise<TranscriptPage> {
    return apiGet(`/meetings/${id}/transcript${buildQueryString({ from, to })}`);
}

export function getMeetingSpeakers(id: string): Promise<SpeakerStat[]> {
    return apiGet(`/meetings/${id}/speakers`);
}

export function getMeetingDialogs(id: string): Promise<Dialog[]> {
    return apiGet(`/meetings/${id}/dialogs`);
}
