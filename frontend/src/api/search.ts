import { apiPost } from "./client";
import type { SearchResponse } from "./types";

export function search(q: string, scope: "all" | "transcripts" | "dialogs" = "all", limit = 20): Promise<SearchResponse> {
    return apiPost("/search", { q, filters: { scope }, pagination: { size: limit } });
}
