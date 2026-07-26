import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ChartTooltipContent } from "@/components/application/charts/charts-base";
import type { StatsSummary } from "@/api/types";
import { formatNumber, queryTypeLabel } from "@/lib/format";

export const QUERY_TYPE_ORDER = ["SPECIFIC", "YES_NO", "GENERAL"] as const;
const QUERY_TYPE_COLORS: Record<string, string> = {
    SPECIFIC: "var(--chart-series-1)",
    YES_NO: "var(--chart-series-2)",
    GENERAL: "var(--chart-series-3)",
};

interface QueryTypeChartProps {
    queryTypeCounts: StatsSummary["queryTypeCounts"];
    /** When supplied, bars become clickable and the cursor reflects it — used to jump to that query type's filtered dialog list. */
    onQueryTypeClick?: (queryType: string) => void;
}

export const QueryTypeChart = ({ queryTypeCounts, onQueryTypeClick }: QueryTypeChartProps) => {
    const data = QUERY_TYPE_ORDER.filter((type) => queryTypeCounts[type] !== undefined).map((type) => ({
        type,
        label: queryTypeLabel(type),
        count: queryTypeCounts[type] ?? 0,
    }));

    return (
        <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data} layout="vertical" margin={{ top: 4, right: 24, bottom: 4, left: 0 }} barCategoryGap={16}>
                    <XAxis type="number" hide />
                    <YAxis
                        type="category"
                        dataKey="label"
                        width={80}
                        tickLine={false}
                        axisLine={false}
                        tick={{ fontSize: 12, fill: "var(--color-text-tertiary, #6b7280)" }}
                    />
                    <Tooltip
                        cursor={{ fill: "var(--chart-grid)" }}
                        content={<ChartTooltipContent formatter={(value) => `${formatNumber(Number(value))} turns`} />}
                    />
                    <Bar
                        dataKey="count"
                        radius={[0, 4, 4, 0]}
                        maxBarSize={28}
                        onClick={onQueryTypeClick ? (entry) => onQueryTypeClick((entry.payload as { type: string }).type) : undefined}
                    >
                        {data.map((entry) => (
                            <Cell key={entry.type} fill={QUERY_TYPE_COLORS[entry.type]} style={{ cursor: onQueryTypeClick ? "pointer" : undefined }} />
                        ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
};
