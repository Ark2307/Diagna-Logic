import { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import type { SortSpec } from "@/api/search-request";
import { useDialogs } from "@/api/queries";
import type { Corpus, QueryType } from "@/api/types";
import { Badge, BadgeWithButton } from "@/components/base/badges/badges";
import { Input } from "@/components/base/input/input";
import { Select } from "@/components/base/select/select";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { PaginationPageMinimalCenter } from "@/components/application/pagination/pagination";
import { QUERY_TYPE_ORDER } from "@/components/charts/query-type-chart";
import { SortableColumnHeader } from "@/components/sortable-column-header";
import { corpusBadgeColor, formatNumber, formatPageRange, queryTypeBadgeColor, queryTypeLabel } from "@/lib/format";

const CORPUS_OPTIONS = [
    { id: "AMI", label: "AMI" },
    { id: "ICSI", label: "ICSI" },
    { id: "PARLIAMENT", label: "Parliament" },
];

const QUERY_TYPE_OPTIONS: { id: QueryType; label: string }[] = [
    { id: "SPECIFIC", label: "Specific" },
    { id: "YES_NO", label: "Yes/no" },
    { id: "GENERAL", label: "General" },
];

export const DialogsPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    // Seeded from the URL so a link with exact filters (e.g. from the dashboard) lands pre-filtered.
    const [meetingId, setMeetingId] = useState(() => searchParams.get("meetingId") ?? "");
    const [corpus, setCorpus] = useState<Corpus | undefined>(() => (searchParams.get("corpus") as Corpus) || undefined);
    const [queryType, setQueryType] = useState<QueryType | undefined>(() => (searchParams.get("queryType") as QueryType) || undefined);
    const [hasUnanswerable, setHasUnanswerable] = useState<boolean | undefined>(() => (searchParams.get("hasUnanswerable") === "true" ? true : undefined));
    const [sort, setSort] = useState<SortSpec | undefined>(undefined);
    const [page, setPage] = useState(0);

    const params = useMemo(
        () => ({
            meetingId: meetingId || undefined,
            corpus,
            queryType,
            hasUnanswerable,
            page,
            size: 20,
            sort: sort ? `${sort.field},${sort.order}` : undefined,
        }),
        [meetingId, corpus, queryType, hasUnanswerable, page, sort],
    );
    const { data, isLoading, isError } = useDialogs(params);

    const handleSortChange = (next: SortSpec) => {
        setSort(next);
        setPage(0);
    };

    return (
        <div className="flex flex-col gap-6">
            <div>
                <h1 className="text-display-xs font-semibold text-primary">Dialogs</h1>
                <p className="mt-1 text-sm text-tertiary">Browse the {data ? formatNumber(data.totalElements) : "432"} information-seeking dialogs.</p>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <Input
                    size="md"
                    label="Meeting ID"
                    placeholder="Exact meeting id..."
                    value={meetingId}
                    onChange={(value) => {
                        setMeetingId(value);
                        setPage(0);
                    }}
                    className="sm:max-w-48"
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
                    label="Query type"
                    placeholder="All query types"
                    items={QUERY_TYPE_OPTIONS}
                    selectedKey={queryType ?? null}
                    onSelectionChange={(key) => {
                        setQueryType((key as QueryType) ?? undefined);
                        setPage(0);
                    }}
                    className="sm:max-w-48"
                >
                    {(item) => <Select.Item id={item.id}>{item.label}</Select.Item>}
                </Select>
            </div>

            {hasUnanswerable && (
                <div className="flex items-center gap-2">
                    <span className="text-sm text-tertiary">Filtered by:</span>
                    <BadgeWithButton
                        type="pill-color"
                        color="brand"
                        buttonLabel="Clear unanswerable filter"
                        onButtonClick={() => {
                            setHasUnanswerable(undefined);
                            setPage(0);
                        }}
                    >
                        Has unanswerable turns
                    </BadgeWithButton>
                </div>
            )}

            {isLoading ? (
                <div className="flex h-64 items-center justify-center">
                    <LoadingIndicator size="md" label="Loading dialogs..." />
                </div>
            ) : isError || !data ? (
                <p className="text-sm text-error-primary">Couldn't load dialogs. Is the backend running?</p>
            ) : data.content.length === 0 ? (
                <div className="flex h-64 flex-col items-center justify-center gap-1 text-center">
                    <p className="text-sm font-medium text-secondary">No dialogs match these filters</p>
                </div>
            ) : (
                <div className="overflow-hidden rounded-xl bg-primary shadow-xs ring-1 ring-secondary">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-secondary">
                                <tr>
                                    <th className="px-6 py-2 text-left text-xs font-semibold whitespace-nowrap text-quaternary">Meeting</th>
                                    <th className="px-6 py-2 text-left text-xs font-semibold whitespace-nowrap text-quaternary">Corpus</th>
                                    <SortableColumnHeader label="Turns" field="turnCount" sort={sort} onSortChange={handleSortChange} align="right" />
                                    <SortableColumnHeader
                                        label="Unanswerable"
                                        field="stats.unanswerableCount"
                                        sort={sort}
                                        onSortChange={handleSortChange}
                                        align="right"
                                    />
                                    <th className="px-6 py-2 text-left text-xs font-semibold whitespace-nowrap text-quaternary">Query types</th>
                                </tr>
                            </thead>
                            <tbody>
                                {data.content.map((dialog) => (
                                    <tr
                                        key={dialog.id}
                                        onClick={() => navigate(`/dialogs/${dialog.id}`)}
                                        className="cursor-pointer border-t border-secondary transition-colors hover:bg-secondary"
                                    >
                                        <td className="px-6 py-4 text-sm font-medium text-primary">{dialog.meetingId}</td>
                                        <td className="px-6 py-4">
                                            <Badge color={corpusBadgeColor(dialog.corpus)} size="sm" type="color">
                                                {dialog.corpus}
                                            </Badge>
                                        </td>
                                        <td className="px-6 py-4 text-right text-sm text-tertiary">{dialog.turnCount}</td>
                                        <td className="px-6 py-4 text-right text-sm text-tertiary">{dialog.stats.unanswerableCount}</td>
                                        <td className="px-6 py-4">
                                            <div className="flex flex-wrap gap-1.5">
                                                {QUERY_TYPE_ORDER.filter((type) => dialog.stats.queryTypeCounts[type]).map((type) => (
                                                    <Badge key={type} color={queryTypeBadgeColor(type as QueryType)} size="sm" type="color">
                                                        {queryTypeLabel(type as QueryType)} · {dialog.stats.queryTypeCounts[type]}
                                                    </Badge>
                                                ))}
                                            </div>
                                        </td>
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
