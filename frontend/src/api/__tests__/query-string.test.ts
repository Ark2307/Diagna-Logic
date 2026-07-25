import { describe, expect, it } from "vitest";
import { buildQueryString } from "../query-string";

describe("buildQueryString", () => {
    it("returns an empty string for an empty params object", () => {
        expect(buildQueryString({})).toBe("");
    });

    it("drops undefined, null, and empty-string values", () => {
        expect(buildQueryString({ a: undefined, b: null, c: "", d: "kept" })).toBe("?d=kept");
    });

    it("returns an empty string (not a bare '?') when everything is dropped", () => {
        expect(buildQueryString({ a: undefined, b: null, c: "" })).toBe("");
    });

    it("serializes strings, numbers, and booleans", () => {
        const result = buildQueryString({ q: "digits", page: 2, includeTranscript: true });
        const params = new URLSearchParams(result.slice(1));
        expect(params.get("q")).toBe("digits");
        expect(params.get("page")).toBe("2");
        expect(params.get("includeTranscript")).toBe("true");
    });

    it("serializes false explicitly rather than dropping it like null/undefined", () => {
        const result = buildQueryString({ hasUnanswerable: false });
        expect(new URLSearchParams(result.slice(1)).get("hasUnanswerable")).toBe("false");
    });

    it("serializes zero explicitly rather than dropping it like an empty string", () => {
        const result = buildQueryString({ minSegments: 0 });
        expect(new URLSearchParams(result.slice(1)).get("minSegments")).toBe("0");
    });

    it("URL-encodes special characters in values", () => {
        const result = buildQueryString({ q: "digits & switchboard" });
        expect(result).toContain("digits+%26+switchboard");
    });
});
