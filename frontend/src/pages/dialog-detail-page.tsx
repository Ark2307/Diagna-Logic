import { useNavigate, useParams } from "react-router";
import { useDialog } from "@/api/queries";
import { Badge } from "@/components/base/badges/badges";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { corpusBadgeColor, queryTypeBadgeColor, queryTypeLabel } from "@/lib/format";

export const DialogDetailPage = () => {
    const { dialogId } = useParams<{ dialogId: string }>();
    const navigate = useNavigate();
    const { data: dialog, isLoading, isError } = useDialog(dialogId, true);

    if (isLoading) {
        return (
            <div className="flex h-64 items-center justify-center">
                <LoadingIndicator size="md" label="Loading dialog..." />
            </div>
        );
    }

    if (isError || !dialog) {
        return <p className="text-sm text-error-primary">Couldn't load this dialog. It may not exist.</p>;
    }

    return (
        <div className="flex flex-col gap-6">
            <div>
                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={() => navigate(`/meetings/${dialog.meetingId}`)}
                        className="text-display-xs font-semibold text-primary hover:text-brand-secondary"
                    >
                        {dialog.meetingId}
                    </button>
                    <Badge color={corpusBadgeColor(dialog.corpus)} size="sm" type="color">
                        {dialog.corpus}
                    </Badge>
                </div>
                <p className="mt-1 text-sm text-tertiary">
                    {dialog.turnCount} turns · {dialog.stats.unanswerableCount} unanswerable · {dialog.stats.attributedTurnCount} attributed
                </p>
            </div>

            <div className="flex flex-col gap-4">
                {dialog.turns.map((turn) => (
                    <div key={turn.turnIndex} className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                        <div className="flex flex-wrap items-center gap-2">
                            <Badge color={queryTypeBadgeColor(turn.queryType)} size="sm" type="color">
                                {queryTypeLabel(turn.queryType)}
                            </Badge>
                            {turn.unanswerable && (
                                <Badge color="warning" size="sm" type="color">
                                    Unanswerable
                                </Badge>
                            )}
                            {turn.contextDependent && (
                                <Badge color="gray" size="sm" type="modern">
                                    Context-dependent
                                </Badge>
                            )}
                        </div>

                        <p className="mt-3 text-sm font-medium text-primary">{turn.query}</p>
                        <p className="mt-1.5 text-sm text-secondary">{turn.response}</p>

                        {turn.resolvedCitations && turn.resolvedCitations.length > 0 && (
                            <div className="mt-3 flex flex-col gap-1.5">
                                <span className="text-xs font-medium text-quaternary">
                                    Cited segment{turn.resolvedCitations.length > 1 ? "s" : ""}
                                </span>
                                <div className="flex flex-wrap gap-1.5">
                                    {turn.resolvedCitations.map((citation) => (
                                        <button
                                            key={citation.segmentIndex}
                                            type="button"
                                            onClick={() => navigate(`/meetings/${dialog.meetingId}?highlight=${citation.segmentIndex}`)}
                                            className="rounded-md bg-brand-secondary px-2 py-1 text-xs font-medium text-brand-secondary transition-colors hover:bg-brand-secondary_hover"
                                            title={citation.text}
                                        >
                                            [{citation.segmentIndex}] {citation.speakerName}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};
