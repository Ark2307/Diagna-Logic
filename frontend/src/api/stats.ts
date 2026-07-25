import { apiGet } from "./client";
import type { MeetingStats, StatsSummary } from "./types";

export function getOverallStats(): Promise<StatsSummary> {
    return apiGet("/stats");
}

export function getMeetingStats(meetingId: string): Promise<MeetingStats> {
    return apiGet(`/stats/meetings/${meetingId}`);
}
