import { useState } from "react";
import { cx } from "@/utils/cx";
import type { NavItemDividerType, NavItemType } from "../config";
import { NavItemBase } from "./nav-item";

interface NavListProps {
    /** URL of the currently active item. */
    activeUrl?: string;
    /** Additional CSS classes to apply to the list. */
    className?: string;
    /** List of items to display. */
    items: (NavItemType | NavItemDividerType)[];
}

export const NavList = ({ activeUrl, items, className }: NavListProps) => {
    // Recomputed every render from activeUrl (not stored in state) — this is a client-side
    // router, so activeUrl changes on every navigation without this component remounting.
    const activeItem = items.find((item) => item.href === activeUrl || item.items?.some((subItem) => subItem.href === activeUrl));
    // Tracks a collapsible section the user opened by hand when none of its children are the active route.
    const [manuallyOpenedItem, setManuallyOpenedItem] = useState<NavItemType>();

    return (
        <ul className={cx("flex flex-col px-4 pt-5", className)}>
            {items.map((item, index) => {
                if (item.divider) {
                    return (
                        <li key={index} className="w-full px-0.5 py-2">
                            <hr className="h-px w-full border-none bg-border-secondary" />
                        </li>
                    );
                }

                if (item.items?.length) {
                    const isOpen = activeItem?.href === item.href || manuallyOpenedItem?.href === item.href;
                    return (
                        <details
                            key={item.label}
                            open={isOpen}
                            className="appearance-none py-0.25"
                            onToggle={(e) => setManuallyOpenedItem(e.currentTarget.open ? item : undefined)}
                        >
                            <NavItemBase href={item.href} badge={item.badge} icon={item.icon} type="collapsible" current={activeItem?.href === item.href}>
                                {item.label}
                            </NavItemBase>

                            <dd>
                                <ul className="pb-1">
                                    {item.items.map((childItem) => (
                                        <li key={childItem.label} className="py-0.25">
                                            <NavItemBase
                                                href={childItem.href}
                                                badge={childItem.badge}
                                                type="collapsible-child"
                                                current={activeUrl === childItem.href}
                                            >
                                                {childItem.label}
                                            </NavItemBase>
                                        </li>
                                    ))}
                                </ul>
                            </dd>
                        </details>
                    );
                }

                return (
                    <li key={item.label} className="py-px">
                        <NavItemBase type="link" badge={item.badge} icon={item.icon} href={item.href} current={activeItem?.href === item.href}>
                            {item.label}
                        </NavItemBase>
                    </li>
                );
            })}
        </ul>
    );
};
