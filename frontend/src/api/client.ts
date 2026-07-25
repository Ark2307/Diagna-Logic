import type { ErrorResponse } from "./types";

/** Thrown for any non-2xx response; carries the parsed backend {@link ErrorResponse} body when available. */
export class ApiError extends Error {
    status: number;
    body: ErrorResponse | null;

    constructor(status: number, body: ErrorResponse | null, message: string) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.body = body;
    }
}

const BASE_URL = "/api/v1";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${BASE_URL}${path}`, {
        ...init,
        headers: {
            "Content-Type": "application/json",
            ...(init?.headers ?? {}),
        },
    });

    if (!response.ok) {
        let body: ErrorResponse | null = null;
        try {
            body = (await response.json()) as ErrorResponse;
        } catch {
            // Response body wasn't JSON (or was empty) — body stays null, the generic message below is used.
        }
        throw new ApiError(response.status, body, body?.message ?? `Request failed with status ${response.status}`);
    }

    if (response.status === 204) {
        return undefined as T;
    }
    return (await response.json()) as T;
}

export function apiGet<T>(path: string): Promise<T> {
    return request<T>(path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
    return request<T>(path, { method: "POST", body: JSON.stringify(body) });
}

export function apiDelete<T>(path: string): Promise<T> {
    return request<T>(path, { method: "DELETE" });
}
