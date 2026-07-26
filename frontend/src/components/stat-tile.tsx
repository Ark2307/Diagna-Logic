import type { FC } from "react";
import { FeaturedIcon } from "@/components/foundations/featured-icon/featured-icon";
import { cx } from "@/utils/cx";

interface StatTileProps {
    label: string;
    value: string;
    icon: FC<{ className?: string }>;
    hint?: string;
    /** When supplied, the tile becomes a keyboard-reachable button that navigates on click/Enter/Space. */
    onClick?: () => void;
}

/** A single dashboard metric — 1-up on mobile, 2-up on tablet, 4-up on desktop via the parent grid's responsive column count. */
export const StatTile = ({ label, value, icon, hint, onClick }: StatTileProps) => {
    return (
        <div
            role={onClick ? "button" : undefined}
            tabIndex={onClick ? 0 : undefined}
            onClick={onClick}
            onKeyDown={
                onClick
                    ? (e) => {
                          if (e.key === "Enter" || e.key === " ") {
                              e.preventDefault();
                              onClick();
                          }
                      }
                    : undefined
            }
            className={cx(
                "flex flex-col gap-4 rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary",
                onClick && "cursor-pointer text-left transition-colors hover:bg-secondary focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
            )}
        >
            <FeaturedIcon icon={icon} color="brand" theme="light" size="md" />
            <div className="flex flex-col gap-1">
                <span className="text-display-xs font-semibold text-primary">{value}</span>
                <span className="text-sm text-tertiary">{label}</span>
                {hint && <span className="text-xs text-quaternary">{hint}</span>}
            </div>
        </div>
    );
};
