import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { useDialogs } from "@/api/queries";
import type { Corpus, QueryType } from "@/api/types";
import { Badge } from "@/components/base/badges/badges";
import { Select } from "@/components/base/select/select";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { PaginationPageMinimalCenter } from "@/components/application/pagination/pagination";
import { corpusBadgeColor, formatNumber, queryTypeBadgeColor, queryTypeLabel } from "@/lib/format";

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
    const [corpus, setCorpus] = useState<Corpus | undefined>();
    const [queryType, setQueryType] = useState<QueryType | undefined>();
    const [page, setPage] = useState(0);

    const params = useMemo(() => ({ corpus, queryType, page, size: 20 }), [corpus, queryType, page]);
    const { data, isLoading, isError } = useDialogs(params);

    return (
        <div className="flex flex-col gap-6">
            <div>
                <h1 className="text-display-xs font-semibold text-primary">Dialogs</h1>
                <p className="mt-1 text-sm text-tertiary">Browse the {data ? formatNumber(data.totalElements) : "432"} information-seeking dialogs.</p>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
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
                                    <th className="px-6 py-2 text-right text-xs font-semibold whitespace-nowrap text-quaternary">Turns</th>
                                    <th className="px-6 py-2 text-right text-xs font-semibold whitespace-nowrap text-quaternary">Unanswerable</th>
                                    <th className="px-6 py-2 text-left text-xs font-semibold whitespace-nowrap text-quaternary">Mostly</th>
                                </tr>
                            </thead>
                            <tbody>
                                {data.content.map((dialog) => {
                                    const topType = Object.entries(dialog.stats.queryTypeCounts).sort((a, b) => b[1] - a[1])[0]?.[0] as
                                        | QueryType
                                        | undefined;
                                    return (
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
                                                {topType && (
                                                    <Badge color={queryTypeBadgeColor(topType)} size="sm" type="color">
                                                        {queryTypeLabel(topType)}
                                                    </Badge>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>

                    {data.totalPages > 1 && (
                        <div className="border-t border-secondary px-4 py-3">
                            <PaginationPageMinimalCenter page={page + 1} total={data.totalPages} onPageChange={(p) => setPage(p - 1)} />
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};
