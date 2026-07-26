import { useState } from "react";
import { SearchLg } from "@untitledui/icons";
import { useMeetings } from "@/api/queries";
import type { Meeting } from "@/api/types";
import { Badge } from "@/components/base/badges/badges";
import { Input } from "@/components/base/input/input";
import { ChatPanel } from "@/components/chat/chat-panel";
import { GeneratePanel } from "@/components/generate/generate-panel";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { corpusBadgeColor } from "@/lib/format";

export const AskPage = () => {
    const [q, setQ] = useState("");
    const [selectedMeeting, setSelectedMeeting] = useState<Meeting | null>(null);
    const { data, isLoading } = useMeetings({ q: q || undefined, size: 8 });

    return (
        <div className="flex flex-col gap-6">
            <div>
                <h1 className="text-display-xs font-semibold text-primary">Ask</h1>
                <p className="mt-1 text-sm text-tertiary">Pick a meeting, then chat with it or generate a summary — the same tools available from any meeting page.</p>
            </div>

            {!selectedMeeting ? (
                <div className="flex flex-col gap-4">
                    <Input
                        size="md"
                        label="Find a meeting"
                        placeholder="Search meetings by transcript content..."
                        icon={SearchLg}
                        value={q}
                        onChange={setQ}
                        className="max-w-md"
                    />

                    {isLoading ? (
                        <LoadingIndicator size="sm" />
                    ) : (
                        <div className="flex flex-col gap-2">
                            {(data?.content ?? []).map((meeting) => (
                                <button
                                    key={meeting.id}
                                    type="button"
                                    onClick={() => setSelectedMeeting(meeting)}
                                    className="flex cursor-pointer items-center justify-between rounded-xl bg-primary p-4 text-left shadow-xs ring-1 ring-secondary transition-colors hover:bg-secondary"
                                >
                                    <span className="flex items-center gap-2">
                                        <span className="text-sm font-medium text-primary">{meeting.id}</span>
                                        <Badge color={corpusBadgeColor(meeting.corpus)} size="sm" type="color">
                                            {meeting.corpus}
                                        </Badge>
                                    </span>
                                    <span className="text-xs text-tertiary">{meeting.segmentCount} segments</span>
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            ) : (
                <div className="flex flex-col gap-4">
                    <button
                        type="button"
                        onClick={() => setSelectedMeeting(null)}
                        className="w-fit cursor-pointer text-sm text-brand-secondary hover:text-brand-secondary_hover"
                    >
                        &larr; Choose a different meeting
                    </button>

                    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                        <div className="flex h-[32rem] flex-col rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                            <h2 className="mb-3 text-sm font-semibold text-primary">Chat — {selectedMeeting.id}</h2>
                            <ChatPanel meetingId={selectedMeeting.id} />
                        </div>
                        <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                            <h2 className="mb-3 text-sm font-semibold text-primary">Generate — {selectedMeeting.id}</h2>
                            <GeneratePanel meetingId={selectedMeeting.id} />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
