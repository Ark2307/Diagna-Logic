/**
 * Types mirroring the backend's JSON response shapes exactly (see
 * backend/src/main/java/com/diagna/logic/api/dto and domain). Kept as one
 * file since these are pure data shapes with no behavior — splitting them
 * up would only make it harder to see the whole contract at a glance.
 */

export type Corpus = "AMI" | "ICSI" | "PARLIAMENT";
export type MeetingDomain = "PRODUCT" | "ACADEMIC" | "PARLIAMENTARY";
export type DatasetSplit = "TRAIN" | "VALIDATION" | "TEST";
export type QueryType = "SPECIFIC" | "YES_NO" | "GENERAL";
export type GenerationTask = "SUMMARY" | "MINUTES" | "DECISIONS" | "ACTION_ITEMS" | "TOPICS" | "CUSTOM";
export type UnanswerableReason = "OUT_OF_SCOPE" | "NOT_IN_TRANSCRIPT" | "NO_VALID_CITATIONS";
export type ChatRole = "USER" | "ASSISTANT";

export interface PageResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface ErrorResponse {
    status: number;
    error: string;
    message: string;
    path: string;
    timestamp: string;
}

export interface SpeakerStat {
    name: string;
    segmentCount: number;
    charCount: number;
}

export interface TranscriptSegment {
    index: number;
    speakerName: string;
    text: string;
}

export interface Meeting {
    id: string;
    corpus: Corpus;
    domain: MeetingDomain;
    split: DatasetSplit;
    segmentCount: number;
    charCount: number;
    estimatedTokens: number;
    speakerCount: number;
    dialogCount: number;
    speakers: SpeakerStat[];
    transcriptSegments?: TranscriptSegment[] | null;
    sourceFile: string;
    ingestedAt: string;
}

export interface TranscriptPage {
    meetingId: string;
    from: number;
    to: number;
    segmentCount: number;
    segments: TranscriptSegment[];
}

export interface AttributionRange {
    startIndex: number;
    endIndex: number;
}

export interface Citation {
    segmentIndex: number;
    speakerName: string;
    text: string;
}

export interface DialogTurn {
    turnIndex: number;
    query: string;
    response: string;
    queryType: QueryType;
    unanswerable: boolean;
    contextDependent: boolean;
    attributionRanges: AttributionRange[];
    attributedSegmentCount: number;
    resolvedCitations?: Citation[] | null;
}

export interface DialogStats {
    unanswerableCount: number;
    attributedTurnCount: number;
    queryTypeCounts: Record<string, number>;
}

export interface Dialog {
    id: string;
    meetingId: string;
    split: DatasetSplit;
    corpus: Corpus;
    domain: MeetingDomain;
    turnCount: number;
    turns: DialogTurn[];
    stats: DialogStats;
    ingestedAt: string;
}

export interface ResolvedCitation {
    startIndex: number;
    endIndex: number;
    segments: Citation[];
}

export interface AttributionResolution {
    dialogId: string;
    turnIndex: number;
    meetingId: string;
    citations: ResolvedCitation[];
}

export interface SearchHit {
    type: "meeting" | "dialog";
    id: string;
    meetingId: string;
    segmentIndex: number | null;
    snippet: string;
    score: number | null;
}

export interface SearchResponse {
    query: string;
    scope: string;
    hits: SearchHit[];
}

export interface SpeakerCount {
    name: string;
    segmentCount: number;
}

export interface MeetingRef {
    meetingId: string;
    segmentCount: number;
}

export interface StatsSummary {
    totalMeetings: number;
    totalDialogs: number;
    totalTurns: number;
    totalSegments: number;
    meetingsByCorpus: Record<string, number>;
    meetingsByDomain: Record<string, number>;
    dialogsBySplit: Record<string, number>;
    queryTypeCounts: Record<string, number>;
    unanswerableTurns: number;
    unanswerableRate: number;
    attributedTurns: number;
    attributionCoverage: number;
    avgTurnsPerDialog: number;
    avgSegmentsPerMeeting: number;
    topSpeakers: SpeakerCount[];
    longestMeeting: MeetingRef | null;
    shortestMeeting: MeetingRef | null;
}

export interface MeetingStats {
    meetingId: string;
    corpus: Corpus;
    domain: MeetingDomain;
    split: DatasetSplit;
    segmentCount: number;
    speakerCount: number;
    dialogCount: number;
    totalTurns: number;
    unanswerableTurns: number;
    attributedTurns: number;
    queryTypeCounts: Record<string, number>;
}

export interface TokenUsage {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
}

export interface ChatRequest {
    meetingId: string;
    message: string;
    conversationId?: string;
    provider?: string;
    model?: string;
}

export interface RetrievalInfo {
    chunkIds: string[];
    topScore: number;
    usedFullTranscript: boolean;
}

export interface ChatResponse {
    conversationId: string;
    answer: string;
    unanswerable: boolean;
    unanswerableReason: UnanswerableReason | null;
    citations: Citation[];
    retrieval: RetrievalInfo;
    provider: string | null;
    model: string | null;
    usage: TokenUsage;
    latencyMs: number;
}

export interface ChatMessage {
    index: number;
    role: ChatRole;
    content: string;
    citations: Citation[];
    unanswerable: boolean;
    unanswerableReason: UnanswerableReason | null;
    retrievedChunkIds: string[];
    provider: string | null;
    model: string | null;
    usage: TokenUsage;
    latencyMs: number;
    createdAt: string;
}

export interface ChatConversation {
    id: string;
    meetingId: string;
    title: string;
    createdAt: string;
    updatedAt: string;
    messages: ChatMessage[];
}

export interface GenerateRequest {
    meetingId?: string;
    dialogId?: string;
    task: GenerationTask;
    instructions?: string;
    maxWords?: number;
    provider?: string;
    model?: string;
}

export interface GenerationStructured {
    overview: string;
    keyPoints: string[];
    decisions: string[];
    actionItems: string[];
    topics: string[];
    participants: string[];
}

export interface GenerateResponse {
    text: string;
    structured: GenerationStructured;
    provider: string;
    model: string;
    usage: TokenUsage;
    latencyMs: number;
    cached: boolean;
}

export interface MeetingSearchParams {
    corpus?: Corpus;
    domain?: MeetingDomain;
    split?: DatasetSplit;
    speaker?: string;
    q?: string;
    minSegments?: number;
    page?: number;
    size?: number;
    sort?: string;
    [key: string]: string | number | boolean | undefined | null;
}

export interface DialogSearchParams {
    meetingId?: string;
    split?: DatasetSplit;
    corpus?: Corpus;
    queryType?: QueryType;
    hasUnanswerable?: boolean;
    minTurns?: number;
    page?: number;
    size?: number;
    sort?: string;
    [key: string]: string | number | boolean | undefined | null;
}
