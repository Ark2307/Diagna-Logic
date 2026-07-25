export type QueryParamValue = string | number | boolean | undefined | null;

/**
 * Builds a URL query string from a params object, dropping any key whose
 * value is {@code undefined}, {@code null}, or an empty string — so callers
 * can pass a full filter object straight through without hand-checking
 * which fields are actually set. Returns {@code ""} (not {@code "?"}) when
 * nothing survives, so it's always safe to append directly to a path.
 */
export function buildQueryString(params: Record<string, QueryParamValue>): string {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
        if (value === undefined || value === null || value === "") {
            continue;
        }
        search.set(key, String(value));
    }
    const serialized = search.toString();
    return serialized ? `?${serialized}` : "";
}
