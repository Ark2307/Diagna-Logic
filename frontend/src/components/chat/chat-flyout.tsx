import { MessageChatCircle } from "@untitledui/icons";
import { Button } from "@/components/base/buttons/button";
import { SlideoutMenu } from "@/components/application/slideout-menus/slideout-menu";
import { ChatPanel } from "./chat-panel";

interface ChatFlyoutProps {
    meetingId: string;
    onCitationClick?: (segmentIndex: number) => void;
}

/** "Ask this meeting" — a meeting-scoped chat thread in a right-side flyout (full-screen on mobile, via SlideoutMenu's own responsive Modal). */
export const ChatFlyout = ({ meetingId, onCitationClick }: ChatFlyoutProps) => {
    return (
        <SlideoutMenu.Trigger>
            <Button color="primary" iconLeading={MessageChatCircle}>
                Ask this meeting
            </Button>
            <SlideoutMenu>
                {({ close }) => (
                    <>
                        <SlideoutMenu.Header onClose={close}>
                            <div className="flex flex-col gap-1.5">
                                <h2 className="text-lg font-semibold text-primary">Ask this meeting</h2>
                                <span className="inline-flex w-fit items-center gap-1 rounded-full bg-brand-secondary px-2 py-0.5 text-xs font-medium text-brand-secondary">
                                    Scoped to {meetingId}
                                </span>
                            </div>
                        </SlideoutMenu.Header>
                        <SlideoutMenu.Content className="min-h-0 flex-1">
                            <ChatPanel meetingId={meetingId} onCitationClick={onCitationClick} />
                        </SlideoutMenu.Content>
                    </>
                )}
            </SlideoutMenu>
        </SlideoutMenu.Trigger>
    );
};
