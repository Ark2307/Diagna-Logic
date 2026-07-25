import type { PropsWithChildren } from "react";
import { Home02, MessageChatCircle, MessageTextSquare02, VideoRecorder } from "@untitledui/icons";
import { useLocation } from "react-router";
import { SidebarNavigationSimple } from "@/components/application/app-navigation/sidebar-navigation/sidebar-simple";
import type { NavItemType } from "@/components/application/app-navigation/config";

const NAV_ITEMS: NavItemType[] = [
    { label: "Dashboard", href: "/", icon: Home02 },
    { label: "Meetings", href: "/meetings", icon: VideoRecorder },
    { label: "Dialogs", href: "/dialogs", icon: MessageTextSquare02 },
    { label: "Ask", href: "/ask", icon: MessageChatCircle },
];

/**
 * The persistent app shell: a fixed sidebar on desktop, a slide-out drawer on
 * mobile (both behaviors come from {@link SidebarNavigationSimple} itself),
 * wrapping every routed page.
 */
export const AppShell = ({ children }: PropsWithChildren) => {
    const location = useLocation();

    return (
        <div className="flex min-h-dvh bg-primary">
            <SidebarNavigationSimple activeUrl={location.pathname} items={NAV_ITEMS} showAccountCard={false} />
            <main className="min-w-0 flex-1">
                <div className="mx-auto max-w-(--breakpoint-2xl) px-4 py-6 md:px-8 md:py-8">{children}</div>
            </main>
        </div>
    );
};
