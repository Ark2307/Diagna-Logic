import { apiGet } from "./client";
import { buildQueryString } from "./query-string";
import type { AttributionResolution, Dialog, DialogSearchParams, PageResponse } from "./types";

export function searchDialogs(params: DialogSearchParams): Promise<PageResponse<Dialog>> {
    return apiGet(`/dialogs${buildQueryString(params)}`);
}

export function getDialog(id: string, resolveAttributions = false): Promise<Dialog> {
    return apiGet(`/dialogs/${id}${buildQueryString({ resolveAttributions })}`);
}

export function getTurnAttribution(dialogId: string, turnIndex: number): Promise<AttributionResolution> {
    return apiGet(`/dialogs/${dialogId}/turns/${turnIndex}/attribution`);
}
