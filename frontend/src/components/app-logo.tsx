import type { HTMLAttributes } from "react";
import { cx } from "@/utils/cx";

/** The app's own wordmark, shown at the top of the sidebar/mobile header in place of the Untitled UI starter-kit logo. */
export const AppLogo = (props: HTMLAttributes<HTMLDivElement>) => {
    return (
        <div {...props} className={cx("flex h-8 w-max items-center", props.className)}>
            <span className="text-lg leading-none font-semibold tracking-tight text-primary">MeetingIQ</span>
        </div>
    );
};
