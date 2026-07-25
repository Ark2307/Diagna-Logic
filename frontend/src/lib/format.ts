import type { Corpus, QueryType } from "@/api/types";

export function formatNumber(value: number): string {
    return new Intl.NumberFormat("en-US").format(value);
}

export function formatPercent(value: number): string {
    return `${(value * 100).toFixed(1)}%`;
}

export function truncate(text: string, maxLength: number): string {
    return text.length <= maxLength ? text : `${text.slice(0, maxLength).trimEnd()}...`;
}

/** Badge color for a corpus — kept distinct per corpus so the dashboard's corpus-mix legend and list-page badges always agree. */
export function corpusBadgeColor(corpus: Corpus): "blue" | "purple" | "orange" {
    switch (corpus) {
        case "AMI":
            return "blue";
        case "ICSI":
            return "purple";
        case "PARLIAMENT":
            return "orange";
    }
}

export function queryTypeBadgeColor(queryType: QueryType): "gray" | "brand" | "success" {
    switch (queryType) {
        case "SPECIFIC":
            return "brand";
        case "YES_NO":
            return "success";
        case "GENERAL":
            return "gray";
    }
}

export function queryTypeLabel(queryType: QueryType): string {
    switch (queryType) {
        case "SPECIFIC":
            return "Specific";
        case "YES_NO":
            return "Yes/no";
        case "GENERAL":
            return "General";
    }
}
