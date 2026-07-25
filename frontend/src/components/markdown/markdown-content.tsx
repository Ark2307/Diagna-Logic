import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { cx } from "@/utils/cx";

interface MarkdownContentProps {
    content: string;
    className?: string;
}

/**
 * Renders LLM-generated Markdown (chat answers, generated summaries) with
 * the scaffold's own `prose` typography tokens, so it stays on-theme in
 * both light and dark mode without a separate styling system.
 */
export const MarkdownContent = ({ content, className }: MarkdownContentProps) => {
    return (
        <div className={cx("prose prose-sm max-w-none text-sm", className)}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
        </div>
    );
};
