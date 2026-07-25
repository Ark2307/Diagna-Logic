import { apiGet } from "./client";
import { buildQueryString } from "./query-string";
import type { Dialog, Meeting, MeetingSearchParams, PageResponse, SpeakerStat, TranscriptPage } from "./types";

export function searchMeetings(params: MeetingSearchParams): Promise<PageResponse<Meeting>> {
    return apiGet(`/meetings${buildQueryString(params)}`);
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
