import { apiGet, apiPost } from "./client";
import { buildQueryString } from "./query-string";
import { parseSort } from "./search-request";
import type { AttributionResolution, Dialog, DialogSearchParams, PageResponse } from "./types";

export function searchDialogs(params: DialogSearchParams): Promise<PageResponse<Dialog>> {
    const { meetingId, split, corpus, queryType, hasUnanswerable, minTurns, page, size, sort } = params;
    return apiPost("/dialogs/search", {
        filters: { meetingId, split, corpus, queryType, hasUnanswerable, minTurns },
        pagination: { page, size },
        sort: parseSort(sort),
    });
}

export function getDialog(id: string, resolveAttributions = false): Promise<Dialog> {
    return apiGet(`/dialogs/${id}${buildQueryString({ resolveAttributions })}`);
}

export function getTurnAttribution(dialogId: string, turnIndex: number): Promise<AttributionResolution> {
    return apiGet(`/dialogs/${dialogId}/turns/${turnIndex}/attribution`);
}
