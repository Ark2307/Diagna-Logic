import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ChartTooltipContent } from "@/components/application/charts/charts-base";
import type { StatsSummary } from "@/api/types";
import { formatNumber } from "@/lib/format";

const CORPUS_LABELS: Record<string, string> = {
    AMI: "AMI (product)",
    ICSI: "ICSI (academic)",
    PARLIAMENT: "Parliamentary",
};

/** Fixed slot order + color per corpus — see dataviz skill: categorical hues assigned by entity, never cycled. */
const CORPUS_ORDER = ["AMI", "ICSI", "PARLIAMENT"] as const;
const CORPUS_COLORS: Record<string, string> = {
    AMI: "var(--chart-series-1)",
    ICSI: "var(--chart-series-2)",
    PARLIAMENT: "var(--chart-series-3)",
};

interface CorpusMixChartProps {
    meetingsByCorpus: StatsSummary["meetingsByCorpus"];
    /** When supplied, bars become clickable and the cursor reflects it — used to jump to that corpus's filtered meeting list. */
    onCorpusClick?: (corpus: string) => void;
}

export const CorpusMixChart = ({ meetingsByCorpus, onCorpusClick }: CorpusMixChartProps) => {
    const data = CORPUS_ORDER.filter((corpus) => meetingsByCorpus[corpus] !== undefined).map((corpus) => ({
        corpus,
        label: CORPUS_LABELS[corpus],
        count: meetingsByCorpus[corpus] ?? 0,
    }));

    return (
        <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data} layout="vertical" margin={{ top: 4, right: 24, bottom: 4, left: 0 }} barCategoryGap={16}>
                    <XAxis type="number" hide />
                    <YAxis
                        type="category"
                        dataKey="label"
                        width={110}
                        tickLine={false}
                        axisLine={false}
                        tick={{ fontSize: 12, fill: "var(--color-text-tertiary, #6b7280)" }}
                    />
                    <Tooltip
                        cursor={{ fill: "var(--chart-grid)" }}
                        content={<ChartTooltipContent formatter={(value) => `${formatNumber(Number(value))} meetings`} />}
                    />
                    <Bar
                        dataKey="count"
                        radius={[0, 4, 4, 0]}
                        maxBarSize={28}
                        onClick={onCorpusClick ? (entry) => onCorpusClick((entry.payload as { corpus: string }).corpus) : undefined}
                    >
                        {data.map((entry) => (
                            <Cell key={entry.corpus} fill={CORPUS_COLORS[entry.corpus]} style={{ cursor: onCorpusClick ? "pointer" : undefined }} />
                        ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
};
