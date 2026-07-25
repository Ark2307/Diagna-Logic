import { MessageChatCircle, Target04, Users01, VideoRecorder } from "@untitledui/icons";
import { useOverallStats } from "@/api/queries";
import { AttributionCoverageChart } from "@/components/charts/attribution-coverage-chart";
import { CorpusMixChart } from "@/components/charts/corpus-mix-chart";
import { QueryTypeChart } from "@/components/charts/query-type-chart";
import { LoadingIndicator } from "@/components/application/loading-indicator/loading-indicator";
import { StatTile } from "@/components/stat-tile";
import { formatNumber, formatPercent } from "@/lib/format";

export const DashboardPage = () => {
    const { data: stats, isLoading, isError } = useOverallStats();

    if (isLoading) {
        return (
            <div className="flex h-64 items-center justify-center">
                <LoadingIndicator size="md" label="Loading dashboard..." />
            </div>
        );
    }

    if (isError || !stats) {
        return <p className="text-sm text-error-primary">Couldn't load dashboard stats. Is the backend running?</p>;
    }

    return (
        <div className="flex flex-col gap-6">
            <div>
                <h1 className="text-display-xs font-semibold text-primary">Dashboard</h1>
                <p className="mt-1 text-sm text-tertiary">An overview of the MISeD meeting transcript corpus.</p>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <StatTile label="Meetings" value={formatNumber(stats.totalMeetings)} icon={VideoRecorder} />
                <StatTile label="Dialogs" value={formatNumber(stats.totalDialogs)} icon={MessageChatCircle} />
                <StatTile label="Turns" value={formatNumber(stats.totalTurns)} icon={Target04} />
                <StatTile label="Speakers" value={formatNumber(stats.topSpeakers.length)} icon={Users01} hint="top speakers tracked" />
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                    <h2 className="text-sm font-semibold text-primary">Corpus mix</h2>
                    <p className="text-xs text-tertiary">Meetings by source corpus</p>
                    <div className="mt-4">
                        <CorpusMixChart meetingsByCorpus={stats.meetingsByCorpus} />
                    </div>
                </div>

                <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                    <h2 className="text-sm font-semibold text-primary">Query type breakdown</h2>
                    <p className="text-xs text-tertiary">Dialog turns by question type</p>
                    <div className="mt-4">
                        <QueryTypeChart queryTypeCounts={stats.queryTypeCounts} />
                    </div>
                </div>

                <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                    <h2 className="text-sm font-semibold text-primary">Attribution coverage</h2>
                    <p className="text-xs text-tertiary">Turns with a cited transcript passage</p>
                    <div className="mt-4">
                        <AttributionCoverageChart attributedTurns={stats.attributedTurns} totalTurns={stats.totalTurns} coverage={stats.attributionCoverage} />
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                    <h2 className="text-sm font-semibold text-primary">Top speakers</h2>
                    <ul className="mt-3 flex flex-col gap-2">
                        {stats.topSpeakers.slice(0, 6).map((speaker) => (
                            <li key={speaker.name} className="flex items-center justify-between text-sm">
                                <span className="text-secondary">{speaker.name}</span>
                                <span className="text-tertiary">{formatNumber(speaker.segmentCount)} segments</span>
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="rounded-xl bg-primary p-5 shadow-xs ring-1 ring-secondary">
                    <h2 className="text-sm font-semibold text-primary">Meeting length extremes</h2>
                    <div className="mt-3 flex flex-col gap-3 text-sm">
                        {stats.longestMeeting && (
                            <div className="flex items-center justify-between">
                                <span className="text-secondary">Longest — {stats.longestMeeting.meetingId}</span>
                                <span className="text-tertiary">{formatNumber(stats.longestMeeting.segmentCount)} segments</span>
                            </div>
                        )}
                        {stats.shortestMeeting && (
                            <div className="flex items-center justify-between">
                                <span className="text-secondary">Shortest — {stats.shortestMeeting.meetingId}</span>
                                <span className="text-tertiary">{formatNumber(stats.shortestMeeting.segmentCount)} segments</span>
                            </div>
                        )}
                        <div className="flex items-center justify-between border-t border-secondary pt-3">
                            <span className="text-secondary">Unanswerable rate</span>
                            <span className="text-tertiary">{formatPercent(stats.unanswerableRate)}</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="text-secondary">Avg turns / dialog</span>
                            <span className="text-tertiary">{stats.avgTurnsPerDialog.toFixed(1)}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};
