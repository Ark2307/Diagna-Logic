import { useMemo, useState } from "react";
import { SearchLg } from "@untitledui/icons";
import { useNavigate, useSearchParams } from "react-router";
import type { SortSpec } from "@/api/search-request";
import { useMeetings } from "@/api/queries";
import type { Corpus, DatasetSplit } from "@/api/types";
import { Badge, BadgeWithButton } from "@/components/base/badges/badges";
import { Input } from "@/components/base/input/input";
import { Select } from "@/components/base/select/select";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { PaginationPageMinimalCenter } from "@/components/application/pagination/pagination";
import { SortableColumnHeader } from "@/components/sortable-column-header";
import { corpusBadgeColor, formatNumber, formatPageRange } from "@/lib/format";

const CORPUS_OPTIONS = [
    { id: "AMI", label: "AMI" },
    { id: "ICSI", label: "ICSI" },
    { id: "PARLIAMENT", label: "Parliament" },
];

const SPLIT_OPTIONS = [
    { id: "TRAIN", label: "Train" },
    { id: "VALIDATION", label: "Validation" },
    { id: "TEST", label: "Test" },
];

export const MeetingsPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    // Seeded from the URL so a link with exact filters (e.g. from the dashboard) lands pre-filtered.
    const [q, setQ] = useState(() => searchParams.get("q") ?? "");
    const [meetingIdQuery, setMeetingIdQuery] = useState(() => searchParams.get("meetingId") ?? "");
    const [corpus, setCorpus] = useState<Corpus | undefined>(() => (searchParams.get("corpus") as Corpus) || undefined);
    const [split, setSplit] = useState<DatasetSplit | undefined>(() => (searchParams.get("split") as DatasetSplit) || undefined);
    const [speaker, setSpeaker] = useState<string | undefined>(() => searchParams.get("speaker") || undefined);
    const [sort, setSort] = useState<SortSpec | undefined>(undefined);
    const [page, setPage] = useState(0);

    const params = useMemo(
        () => ({
            q: q || undefined,
            meetingId: meetingIdQuery || undefined,
            corpus,
            split,
            speaker,
            page,
            size: 20,
            sort: sort ? `${sort.field},${sort.order}` : undefined,
        }),
        [q, meetingIdQuery, corpus, split, speaker, page, sort],
    );
    const { data, isLoading, isError } = useMeetings(params);

    const handleSortChange = (next: SortSpec) => {
        setSort(next);
        setPage(0);
    };

    return (
        <div className="flex flex-col gap-6">
            <div>
                <h1 className="text-display-xs font-semibold text-primary">Meetings</h1>
                <p className="mt-1 text-sm text-tertiary">Browse the {data ? formatNumber(data.totalElements) : "225"} meeting transcripts.</p>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <Input
                    size="md"
                    label="Meeting ID"
                    placeholder="Search by meeting id..."
                    value={meetingIdQuery}
                    onChange={(value) => {
                        setMeetingIdQuery(value);
                        setPage(0);
                    }}
                    className="sm:max-w-48"
                />
                <Input
                    size="md"
                    label="Search transcripts"
                    placeholder="Search transcript text..."
                    icon={SearchLg}
                    value={q}
                    onChange={(value) => {
                        setQ(value);
                        setPage(0);
                    }}
                    className="sm:max-w-xs"
                />
                <Select
                    size="md"
                    label="Corpus"
                    placeholder="All corpora"
                    items={CORPUS_OPTIONS}
                    selectedKey={corpus ?? null}
                    onSelectionChange={(key) => {
                        setCorpus((key as Corpus) ?? undefined);
                        setPage(0);
                    }}
                    className="sm:max-w-40"
                >
                    {(item) => <Select.Item id={item.id}>{item.label}</Select.Item>}
                </Select>
                <Select
                    size="md"
                    label="Split"
                    placeholder="All splits"
                    items={SPLIT_OPTIONS}
                    selectedKey={split ?? null}
                    onSelectionChange={(key) => {
                        setSplit((key as DatasetSplit) ?? undefined);
                        setPage(0);
                    }}
                    className="sm:max-w-40"
                >
                    {(item) => <Select.Item id={item.id}>{item.label}</Select.Item>}
                </Select>
            </div>

            {speaker && (
                <div className="flex items-center gap-2">
                    <span className="text-sm text-tertiary">Filtered by speaker:</span>
                    <BadgeWithButton
                        type="pill-color"
                        color="brand"
                        buttonLabel={`Clear speaker filter (${speaker})`}
                        onButtonClick={() => {
                            setSpeaker(undefined);
                            setPage(0);
                        }}
                    >
                        {speaker}
                    </BadgeWithButton>
                </div>
            )}

            {isLoading ? (
                <div className="flex h-64 items-center justify-center">
                    <LoadingIndicator size="md" label="Loading meetings..." />
                </div>
            ) : isError || !data ? (
                <p className="text-sm text-error-primary">Couldn't load meetings. Is the backend running?</p>
            ) : data.content.length === 0 ? (
                <div className="flex h-64 flex-col items-center justify-center gap-1 text-center">
                    <p className="text-sm font-medium text-secondary">No meetings match these filters</p>
                    <p className="text-sm text-tertiary">Try clearing the search or corpus filter.</p>
                </div>
            ) : (
                <div className="overflow-hidden rounded-xl bg-primary shadow-xs ring-1 ring-secondary">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-secondary">
                                <tr>
                                    <th className="px-6 py-2 text-left text-xs font-semibold whitespace-nowrap text-quaternary">Meeting</th>
                                    <th className="px-6 py-2 text-left text-xs font-semibold whitespace-nowrap text-quaternary">Corpus</th>
                                    <th className="px-6 py-2 text-left text-xs font-semibold whitespace-nowrap text-quaternary">Split</th>
                                    <SortableColumnHeader label="Segments" field="segmentCount" sort={sort} onSortChange={handleSortChange} align="right" />
                                    <SortableColumnHeader label="Speakers" field="speakerCount" sort={sort} onSortChange={handleSortChange} align="right" />
                                    <SortableColumnHeader label="Dialogs" field="dialogCount" sort={sort} onSortChange={handleSortChange} align="right" />
                                </tr>
                            </thead>
                            <tbody>
                                {data.content.map((meeting) => (
                                    <tr
                                        key={meeting.id}
                                        onClick={() => navigate(`/meetings/${meeting.id}`)}
                                        className="cursor-pointer border-t border-secondary transition-colors hover:bg-secondary"
                                    >
                                        <td className="px-6 py-4 text-sm font-medium text-primary">{meeting.id}</td>
                                        <td className="px-6 py-4">
                                            <Badge color={corpusBadgeColor(meeting.corpus)} size="sm" type="color">
                                                {meeting.corpus}
                                            </Badge>
                                        </td>
                                        <td className="px-6 py-4 text-sm text-tertiary">{meeting.split}</td>
                                        <td className="px-6 py-4 text-right text-sm text-tertiary">{formatNumber(meeting.segmentCount)}</td>
                                        <td className="px-6 py-4 text-right text-sm text-tertiary">{meeting.speakerCount}</td>
                                        <td className="px-6 py-4 text-right text-sm text-tertiary">{meeting.dialogCount}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <div className="flex flex-col items-center justify-between gap-3 border-t border-secondary px-4 py-3 sm:flex-row">
                        <span className="text-sm text-tertiary">Showing {formatPageRange(page, 20, data.totalElements)}</span>
                        {data.totalPages > 1 && (
                            <PaginationPageMinimalCenter page={page + 1} total={data.totalPages} onPageChange={(p) => setPage(p - 1)} />
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};
