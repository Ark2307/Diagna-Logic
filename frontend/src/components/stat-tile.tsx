import type { FC } from "react";
import { FeaturedIcon } from "@/components/foundations/featured-icon/featured-icon";

interface StatTileProps {
    label: string;
    value: string;
    icon: FC<{ className?: string }>;
    hint?: string;
}

/** A single dashboard metric — 1-up on mobile, 2-up on tablet, 4-up on desktop via the parent grid's responsive column count. */
export const StatTile = ({ label, value, icon, hint }: StatTileProps) => {
    return (
        <div className="flex flex-col gap-4 rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
            <FeaturedIcon icon={icon} color="brand" theme="light" size="md" />
            <div className="flex flex-col gap-1">
                <span className="text-display-xs font-semibold text-primary">{value}</span>
                <span className="text-sm text-tertiary">{label}</span>
                {hint && <span className="text-xs text-quaternary">{hint}</span>}
            </div>
        </div>
    );
};
