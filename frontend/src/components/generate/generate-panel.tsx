import { useState } from "react";
import { MagicWand02 } from "@untitledui/icons";
import { useGenerateText } from "@/api/queries";
import type { GenerateResponse, GenerationTask } from "@/api/types";
import { Button } from "@/components/base/buttons/button";
import { Select } from "@/components/base/select/select";
import { TextArea } from "@/components/base/textarea/textarea";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { MarkdownContent } from "@/components/markdown/markdown-content";

const TASK_OPTIONS: { id: GenerationTask; label: string }[] = [
    { id: "SUMMARY", label: "Summary" },
    { id: "MINUTES", label: "Meeting minutes" },
    { id: "DECISIONS", label: "Decisions" },
    { id: "ACTION_ITEMS", label: "Action items" },
    { id: "TOPICS", label: "Topics" },
    { id: "CUSTOM", label: "Custom instructions" },
];

interface GeneratePanelProps {
    meetingId: string;
}

function renderList(items: string[]) {
    if (items.length === 0) return null;
    return <MarkdownContent content={items.map((item) => `- ${item}`).join("\n")} />;
}

export const GeneratePanel = ({ meetingId }: GeneratePanelProps) => {
    const [task, setTask] = useState<GenerationTask>("SUMMARY");
    const [instructions, setInstructions] = useState("");
    const [result, setResult] = useState<GenerateResponse | null>(null);
    const generate = useGenerateText();

    const handleGenerate = () => {
        generate.mutate(
            { meetingId, task, instructions: instructions.trim() || undefined },
            { onSuccess: (response) => setResult(response) },
        );
    };

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <Select size="md" label="Generate" items={TASK_OPTIONS} selectedKey={task} onSelectionChange={(key) => setTask(key as GenerationTask)} className="sm:max-w-56">
                    {(item) => <Select.Item id={item.id}>{item.label}</Select.Item>}
                </Select>
                <Button color="secondary" iconLeading={MagicWand02} onClick={handleGenerate} isLoading={generate.isPending}>
                    Generate
                </Button>
            </div>

            {task === "CUSTOM" && (
                <TextArea
                    aria-label="Custom instructions"
                    label="Instructions"
                    placeholder="e.g. Summarize this meeting for someone who missed it, focusing on decisions."
                    rows={2}
                    value={instructions}
                    onChange={setInstructions}
                />
            )}

            {generate.isPending && (
                <div className="flex items-center gap-2 py-4">
                    <LoadingIndicator size="sm" />
                    <span className="text-sm text-tertiary">Generating...</span>
                </div>
            )}

            {generate.isError && <p className="text-sm text-error-primary">Something went wrong generating that. Try again.</p>}

            {result && (
                <div className="flex flex-col gap-4 rounded-xl bg-secondary p-4">
                    <MarkdownContent content={result.text} />

                    {result.structured.keyPoints.length > 0 && (
                        <div>
                            <h3 className="mb-1 text-xs font-semibold text-quaternary">Key points</h3>
                            {renderList(result.structured.keyPoints)}
                        </div>
                    )}
                    {result.structured.decisions.length > 0 && (
                        <div>
                            <h3 className="mb-1 text-xs font-semibold text-quaternary">Decisions</h3>
                            {renderList(result.structured.decisions)}
                        </div>
                    )}
                    {result.structured.actionItems.length > 0 && (
                        <div>
                            <h3 className="mb-1 text-xs font-semibold text-quaternary">Action items</h3>
                            {renderList(result.structured.actionItems)}
                        </div>
                    )}
                    {result.structured.topics.length > 0 && (
                        <div>
                            <h3 className="mb-1 text-xs font-semibold text-quaternary">Topics</h3>
                            {renderList(result.structured.topics)}
                        </div>
                    )}

                    <div className="flex items-center gap-3 border-t border-secondary pt-3 text-xs text-quaternary">
                        <span>
                            {result.provider} / {result.model}
                        </span>
                        <span>{result.usage.totalTokens} tokens</span>
                        <span>{result.latencyMs}ms</span>
                        {result.cached && <span>cached</span>}
                    </div>
                </div>
            )}
        </div>
    );
};
