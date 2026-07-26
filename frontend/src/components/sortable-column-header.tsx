import { ArrowDown, ArrowUp, SwitchVertical01 } from "@untitledui/icons";
import type { SortSpec } from "@/api/search-request";
import { cx } from "@/utils/cx";

interface SortableColumnHeaderProps {
    label: string;
    field: string;
    sort: SortSpec | undefined;
    onSortChange: (sort: SortSpec) => void;
    align?: "left" | "right";
}

/** A `<th>` whose label toggles asc/desc sort on that column when clicked — shared by the meetings and dialogs tables. */
export const SortableColumnHeader = ({ label, field, sort, onSortChange, align = "left" }: SortableColumnHeaderProps) => {
    const isActive = sort?.field === field;
    const nextOrder: SortSpec["order"] = isActive && sort.order === "asc" ? "desc" : "asc";
    const Icon = isActive ? (sort.order === "asc" ? ArrowUp : ArrowDown) : SwitchVertical01;

    return (
        <th className={cx("px-6 py-2 text-xs font-semibold whitespace-nowrap text-quaternary", align === "right" ? "text-right" : "text-left")}>
            <button
                type="button"
                onClick={() => onSortChange({ field, order: nextOrder })}
                className={cx(
                    "inline-flex items-center gap-1 rounded-sm transition-colors hover:text-secondary focus:outline-none focus-visible:ring-2 focus-visible:ring-brand",
                    align === "right" && "flex-row-reverse",
                    isActive && "text-secondary",
                )}
            >
                {label}
                <Icon className="size-3" aria-hidden="true" />
            </button>
        </th>
    );
};
