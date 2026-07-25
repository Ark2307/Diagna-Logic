import { apiGet } from "./client";
import { buildQueryString } from "./query-string";
import type { SearchResponse } from "./types";

export function search(q: string, scope: "all" | "transcripts" | "dialogs" = "all", limit = 20): Promise<SearchResponse> {
    return apiGet(`/search${buildQueryString({ q, scope, limit })}`);
}
