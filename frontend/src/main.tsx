import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Route, Routes } from "react-router";
import { AppShell } from "@/components/app-shell";
import { AskPage } from "@/pages/ask-page";
import { DashboardPage } from "@/pages/dashboard-page";
import { DialogDetailPage } from "@/pages/dialog-detail-page";
import { DialogsPage } from "@/pages/dialogs-page";
import { MeetingDetailPage } from "@/pages/meeting-detail-page";
import { MeetingsPage } from "@/pages/meetings-page";
import { NotFound } from "@/pages/not-found";
import { QueryProvider } from "@/providers/query-provider";
import { RouteProvider } from "@/providers/router-provider";
import { ThemeProvider } from "@/providers/theme-provider";
import "@/styles/globals.css";

createRoot(document.getElementById("root")!).render(
    <StrictMode>
        <ThemeProvider>
            <QueryProvider>
                <BrowserRouter>
                    <RouteProvider>
                        <Routes>
                            <Route
                                path="/"
                                element={
                                    <AppShell>
                                        <DashboardPage />
                                    </AppShell>
                                }
                            />
                            <Route
                                path="/meetings"
                                element={
                                    <AppShell>
                                        <MeetingsPage />
                                    </AppShell>
                                }
                            />
                            <Route
                                path="/meetings/:meetingId"
                                element={
                                    <AppShell>
                                        <MeetingDetailPage />
                                    </AppShell>
                                }
                            />
                            <Route
                                path="/dialogs"
                                element={
                                    <AppShell>
                                        <DialogsPage />
                                    </AppShell>
                                }
                            />
                            <Route
                                path="/dialogs/:dialogId"
                                element={
                                    <AppShell>
                                        <DialogDetailPage />
                                    </AppShell>
                                }
                            />
                            <Route
                                path="/ask"
                                element={
                                    <AppShell>
                                        <AskPage />
                                    </AppShell>
                                }
                            />
                            <Route path="*" element={<NotFound />} />
                        </Routes>
                    </RouteProvider>
                </BrowserRouter>
            </QueryProvider>
        </ThemeProvider>
    </StrictMode>,
);
