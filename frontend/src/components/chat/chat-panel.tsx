import { useEffect, useRef, useState } from "react";
import { AlertCircle, Send01 } from "@untitledui/icons";
import { useConversations, useMeetingDialogs, useSendChatMessage } from "@/api/queries";
import type { ChatMessage } from "@/api/types";
import { Button } from "@/components/base/buttons/button";
import { TextArea } from "@/components/base/textarea/textarea";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { MarkdownContent } from "@/components/markdown/markdown-content";

interface ChatPanelProps {
    meetingId: string;
    onCitationClick?: (segmentIndex: number) => void;
}

const UNANSWERABLE_COPY: Record<string, string> = {
    OUT_OF_SCOPE: "That isn't discussed in this meeting.",
    NOT_IN_TRANSCRIPT: "The transcript doesn't contain a clear answer to that.",
    NO_VALID_CITATIONS: "I couldn't find a well-supported answer to that in this meeting.",
};

/**
 * The chat message list + composer, used inside {@link ChatFlyout}. Kept
 * separate from the flyout chrome so the same conversation UI could be
 * reused in a non-flyout context (e.g. the /ask playground) without
 * duplicating the message-rendering logic.
 */
export const ChatPanel = ({ meetingId, onCitationClick }: ChatPanelProps) => {
    const [conversationId, setConversationId] = useState<string | undefined>();
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState("");
    const sendMessage = useSendChatMessage();
    const { data: dialogs } = useMeetingDialogs(meetingId);
    const { data: conversations } = useConversations(meetingId);
    const listEndRef = useRef<HTMLDivElement>(null);

    // Restore the meeting's most recent conversation on mount (conversations are ordered
    // newest-first) — without this, reopening the chat always looks like a blank thread even
    // though the history is already persisted server-side.
    useEffect(() => {
        if (conversationId || messages.length > 0) return;
        if (!conversations || conversations.length === 0) return;
        const latest = conversations[0];
        setConversationId(latest.id);
        setMessages(latest.messages);
    }, [conversations, conversationId, messages.length]);

    useEffect(() => {
        listEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages.length]);

    const starterChips = (dialogs ?? [])
        .flatMap((dialog) => dialog.turns.map((turn) => turn.query))
        .filter((query, index, all) => all.indexOf(query) === index)
        .slice(0, 4);

    const handleSend = (text: string) => {
        const trimmed = text.trim();
        if (!trimmed || sendMessage.isPending) return;

        const userMessage: ChatMessage = {
            index: messages.length,
            role: "USER",
            content: trimmed,
            citations: [],
            unanswerable: false,
            unanswerableReason: null,
            retrievedChunkIds: [],
            provider: null,
            model: null,
            usage: { promptTokens: 0, completionTokens: 0, totalTokens: 0 },
            latencyMs: 0,
            createdAt: new Date(0).toISOString(),
        };
        setMessages((prev) => [...prev, userMessage]);
        setInput("");

        sendMessage.mutate(
            { meetingId, message: trimmed, conversationId },
            {
                onSuccess: (response) => {
                    setConversationId(response.conversationId);
                    const assistantMessage: ChatMessage = {
                        index: messages.length + 1,
                        role: "ASSISTANT",
                        content: response.answer,
                        citations: response.citations,
                        unanswerable: response.unanswerable,
                        unanswerableReason: response.unanswerableReason,
                        retrievedChunkIds: response.retrieval.chunkIds,
                        provider: response.provider,
                        model: response.model,
                        usage: response.usage,
                        latencyMs: response.latencyMs,
                        createdAt: new Date(0).toISOString(),
                    };
                    setMessages((prev) => [...prev, assistantMessage]);
                },
            },
        );
    };

    return (
        <div className="flex h-full flex-col gap-4">
            <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto pb-2">
                {messages.length === 0 && (
                    <div className="flex flex-col gap-3">
                        <p className="text-sm text-tertiary">
                            Ask a question about this meeting. Answers are grounded in the transcript and cite the exact segments they come from.
                        </p>
                        {starterChips.length > 0 && (
                            <div className="flex flex-col gap-2">
                                <span className="text-xs font-medium text-quaternary">Try asking</span>
                                <div className="flex flex-col gap-2">
                                    {starterChips.map((query) => (
                                        <button
                                            key={query}
                                            type="button"
                                            onClick={() => handleSend(query)}
                                            className="cursor-pointer rounded-lg border border-secondary bg-secondary px-3 py-2 text-left text-sm text-secondary transition-colors hover:bg-secondary_hover"
                                        >
                                            {query}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {messages.map((message, i) => (
                    <div key={i} className={message.role === "USER" ? "flex justify-end" : "flex justify-start"}>
                        {message.role === "USER" ? (
                            <div className="max-w-[85%] rounded-xl rounded-tr-sm bg-brand-solid px-4 py-2.5 text-sm text-white">{message.content}</div>
                        ) : (
                            <div className="flex max-w-[90%] flex-col gap-2">
                                {message.unanswerable ? (
                                    <div className="flex items-start gap-2 rounded-xl rounded-tl-sm bg-warning-secondary px-4 py-2.5">
                                        <AlertCircle className="mt-0.5 size-4 shrink-0 text-fg-warning-secondary" aria-hidden="true" />
                                        <span className="text-sm text-primary">
                                            {message.unanswerableReason ? UNANSWERABLE_COPY[message.unanswerableReason] : message.content}
                                        </span>
                                    </div>
                                ) : (
                                    <div className="rounded-xl rounded-tl-sm bg-secondary px-4 py-2.5">
                                        <MarkdownContent content={message.content} />
                                    </div>
                                )}

                                {message.citations.length > 0 && (
                                    <div className="flex flex-col gap-1.5 px-1">
                                        <span className="text-xs text-quaternary">
                                            Answered from {message.citations.length} passage{message.citations.length > 1 ? "s" : ""}
                                        </span>
                                        <div className="flex flex-wrap gap-1.5">
                                            {message.citations.map((citation) => (
                                                <button
                                                    key={citation.segmentIndex}
                                                    type="button"
                                                    onClick={() => onCitationClick?.(citation.segmentIndex)}
                                                    className="cursor-pointer rounded-md bg-brand-secondary px-2 py-1 text-xs font-medium text-brand-secondary transition-colors hover:bg-brand-secondary_hover"
                                                    title={citation.text}
                                                >
                                                    [{citation.segmentIndex}] {citation.speakerName}
                                                </button>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                ))}

                {sendMessage.isPending && (
                    <div className="flex justify-start">
                        <div className="rounded-xl rounded-tl-sm bg-secondary px-4 py-3">
                            <LoadingIndicator size="sm" />
                        </div>
                    </div>
                )}

                {sendMessage.isError && <p className="text-sm text-error-primary">Something went wrong sending that message. Try again.</p>}

                <div ref={listEndRef} />
            </div>

            <form
                className="flex items-end gap-2 border-t border-secondary pt-4"
                onSubmit={(e) => {
                    e.preventDefault();
                    handleSend(input);
                }}
            >
                <TextArea
                    aria-label="Message"
                    placeholder="Ask a question about this meeting..."
                    rows={2}
                    value={input}
                    onChange={setInput}
                    onKeyDown={(e) => {
                        if (e.key === "Enter" && !e.shiftKey) {
                            e.preventDefault();
                            handleSend(input);
                        }
                    }}
                    className="flex-1"
                />
                <Button type="submit" size="md" iconLeading={Send01} isDisabled={!input.trim() || sendMessage.isPending} aria-label="Send message" />
            </form>
        </div>
    );
};
