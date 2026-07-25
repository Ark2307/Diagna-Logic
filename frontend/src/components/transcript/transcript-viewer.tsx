import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "@untitledui/icons";
import { useTranscriptPage } from "@/api/queries";
import { Button } from "@/components/base/buttons/button";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { formatNumber } from "@/lib/format";

const WINDOW_SIZE = 200;

interface TranscriptViewerProps {
    meetingId: string;
    segmentCount: number;
    /** A segment index to jump to and highlight — e.g. from a chat citation click. */
    highlightIndex?: number | null;
}

/**
 * A windowed (not virtualized) transcript reader: it pages through the
 * backend's own {@code /transcript?from&to} slice endpoint rather than
 * fetching all up-to-1,530 segments and virtualizing the DOM client-side.
 * Same practical goal (never rendering the whole transcript at once)
 * reached with the pagination the backend already exposes, at the cost of
 * a page boundary instead of smooth infinite scroll.
 */
export const TranscriptViewer = ({ meetingId, segmentCount, highlightIndex }: TranscriptViewerProps) => {
    const [windowFrom, setWindowFrom] = useState(0);
    const highlightRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (highlightIndex == null) return;
        const windowStart = Math.max(0, Math.floor(highlightIndex / WINDOW_SIZE) * WINDOW_SIZE);
        setWindowFrom(windowStart);
    }, [highlightIndex]);

    const windowTo = Math.min(segmentCount - 1, windowFrom + WINDOW_SIZE - 1);
    const { data, isLoading } = useTranscriptPage(meetingId, windowFrom, windowTo);

    useEffect(() => {
        if (highlightIndex != null && data) {
            highlightRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    }, [highlightIndex, data]);

    const hasPrev = windowFrom > 0;
    const hasNext = windowTo < segmentCount - 1;

    return (
        <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
                <span className="text-xs text-quaternary">
                    Segments {formatNumber(windowFrom)}–{formatNumber(windowTo)} of {formatNumber(segmentCount)}
                </span>
                <div className="flex gap-2">
                    <Button size="sm" color="tertiary" iconLeading={ChevronLeft} isDisabled={!hasPrev} onClick={() => setWindowFrom((f) => Math.max(0, f - WINDOW_SIZE))}>
                        Previous
                    </Button>
                    <Button
                        size="sm"
                        color="tertiary"
                        iconTrailing={ChevronRight}
                        isDisabled={!hasNext}
                        onClick={() => setWindowFrom((f) => f + WINDOW_SIZE)}
                    >
                        Next
                    </Button>
                </div>
            </div>

            <div className="max-h-[32rem] overflow-y-auto rounded-xl bg-secondary p-4">
                {isLoading || !data ? (
                    <div className="flex h-32 items-center justify-center">
                        <LoadingIndicator size="sm" />
                    </div>
                ) : (
                    <div className="flex flex-col gap-2">
                        {data.segments.map((segment) => (
                            <div
                                key={segment.index}
                                ref={segment.index === highlightIndex ? highlightRef : undefined}
                                className={`rounded-lg px-2 py-1 text-sm ${
                                    segment.index === highlightIndex ? "bg-brand-secondary ring-1 ring-brand" : ""
                                }`}
                            >
                                <span className="mr-2 font-mono text-xs text-quaternary">[{segment.index}]</span>
                                <span className="font-medium text-secondary">{segment.speakerName}:</span>{" "}
                                <span className="text-primary">{segment.text}</span>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};
