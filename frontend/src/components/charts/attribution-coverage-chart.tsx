import { formatNumber, formatPercent } from "@/lib/format";

interface AttributionCoverageChartProps {
    attributedTurns: number;
    totalTurns: number;
    coverage: number;
}

/**
 * A single proportion (attributed vs. not) is a headline metric, not a
 * multi-category comparison — per the dataviz skill, that's a stat number
 * plus a simple two-segment bar, not a chart library. The accent color
 * (attributed) is the same blue used as slot 1 across the dashboard's other
 * charts; "not attributed" is a neutral gray absence state, not a second
 * competing series.
 */
export const AttributionCoverageChart = ({ attributedTurns, totalTurns, coverage }: AttributionCoverageChartProps) => {
    const attributedPercent = Math.round(coverage * 1000) / 10;
    const notAttributed = totalTurns - attributedTurns;

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-baseline gap-2">
                <span className="text-display-sm font-semibold text-primary">{formatPercent(coverage)}</span>
                <span className="text-sm text-tertiary">of turns cite a transcript passage</span>
            </div>

            <div
                role="img"
                aria-label={`${formatNumber(attributedTurns)} of ${formatNumber(totalTurns)} turns are attributed (${attributedPercent}%)`}
                className="flex h-3 w-full overflow-hidden rounded-full bg-secondary"
            >
                <div className="h-full" style={{ width: `${attributedPercent}%`, backgroundColor: "var(--chart-series-1)" }} />
            </div>

            <div className="flex items-center justify-between text-sm">
                <span className="flex items-center gap-1.5 text-tertiary">
                    <span className="inline-block size-2 rounded-full" style={{ backgroundColor: "var(--chart-series-1)" }} />
                    Attributed — {formatNumber(attributedTurns)}
                </span>
                <span className="flex items-center gap-1.5 text-tertiary">
                    <span className="inline-block size-2 rounded-full" style={{ backgroundColor: "var(--chart-neutral)" }} />
                    Not attributed — {formatNumber(notAttributed)}
                </span>
            </div>
        </div>
    );
};
