import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { useMeeting, useMeetingDialogs } from "@/api/queries";
import type { QueryType } from "@/api/types";
import { Badge } from "@/components/base/badges/badges";
import { ChatFlyout } from "@/components/chat/chat-flyout";
import { GeneratePanel } from "@/components/generate/generate-panel";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { TranscriptViewer } from "@/components/transcript/transcript-viewer";
import { corpusBadgeColor, formatNumber, queryTypeBadgeColor, queryTypeLabel } from "@/lib/format";

export const MeetingDetailPage = () => {
    const { meetingId } = useParams<{ meetingId: string }>();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [highlightIndex, setHighlightIndex] = useState<number | null>(null);
    const { data: meeting, isLoading, isError } = useMeeting(meetingId);
    const { data: dialogs } = useMeetingDialogs(meetingId);

    useEffect(() => {
        const fromUrl = searchParams.get("highlight");
        if (fromUrl !== null) {
            const parsed = Number(fromUrl);
            if (Number.isInteger(parsed)) {
                setHighlightIndex(parsed);
            }
        }
        // Only re-run when the meeting changes or the URL's highlight value changes —
        // deliberately not on every searchParams identity change.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [meetingId, searchParams.get("highlight")]);

    if (isLoading) {
        return (
            <div className="flex h-64 items-center justify-center">
                <LoadingIndicator size="md" label="Loading meeting..." />
            </div>
        );
    }

    if (isError || !meeting || !meetingId) {
        return <p className="text-sm text-error-primary">Couldn't load this meeting. It may not exist.</p>;
    }

    return (
        <div className="flex flex-col gap-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                    <div className="flex items-center gap-2">
                        <h1 className="text-display-xs font-semibold text-primary">{meeting.id}</h1>
                        <Badge color={corpusBadgeColor(meeting.corpus)} size="sm" type="color">
                            {meeting.corpus}
                        </Badge>
                    </div>
                    <p className="mt-1 text-sm text-tertiary">
                        {formatNumber(meeting.segmentCount)} segments · {meeting.speakerCount} speakers · {meeting.dialogCount} dialog
                        {meeting.dialogCount !== 1 ? "s" : ""} · {meeting.split.toLowerCase()} split
                    </p>
                </div>
                <ChatFlyout meetingId={meetingId} onCitationClick={setHighlightIndex} />
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                <div className="flex flex-col gap-4 lg:col-span-2">
                    <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                        <h2 className="text-sm font-semibold text-primary">Transcript</h2>
                        <div className="mt-3">
                            <TranscriptViewer meetingId={meetingId} segmentCount={meeting.segmentCount} highlightIndex={highlightIndex} />
                        </div>
                    </div>
                </div>

                <div className="flex flex-col gap-4">
                    <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                        <h2 className="text-sm font-semibold text-primary">Generate</h2>
                        <div className="mt-3">
                            <GeneratePanel meetingId={meetingId} />
                        </div>
                    </div>

                    <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                        <h2 className="text-sm font-semibold text-primary">Speakers</h2>
                        <ul className="mt-3 flex flex-col gap-2">
                            {meeting.speakers.map((speaker) => (
                                <li key={speaker.name} className="flex items-center justify-between text-sm">
                                    <span className="text-secondary">{speaker.name}</span>
                                    <span className="text-tertiary">{formatNumber(speaker.segmentCount)} segs</span>
                                </li>
                            ))}
                        </ul>
                    </div>

                    {dialogs && dialogs.length > 0 && (
                        <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                            <h2 className="text-sm font-semibold text-primary">Dialogs</h2>
                            <ul className="mt-3 flex flex-col gap-2">
                                {dialogs.map((dialog) => {
                                    const topType = Object.entries(dialog.stats.queryTypeCounts).sort((a, b) => b[1] - a[1])[0]?.[0];
                                    return (
                                        <li key={dialog.id}>
                                            <button
                                                type="button"
                                                onClick={() => navigate(`/dialogs/${dialog.id}`)}
                                                className="flex w-full cursor-pointer items-center justify-between rounded-lg px-2 py-1.5 text-left text-sm transition-colors hover:bg-secondary"
                                            >
                                                <span className="text-secondary">{dialog.turnCount} turns</span>
                                                {topType && (
                                                    <Badge color={queryTypeBadgeColor(topType as QueryType)} size="sm" type="color">
                                                        {queryTypeLabel(topType as QueryType)}
                                                    </Badge>
                                                )}
                                            </button>
                                        </li>
                                    );
                                })}
                            </ul>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
