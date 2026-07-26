package com.meetingiq.platform.rag;

import com.meetingiq.platform.domain.Citation;
import com.meetingiq.platform.domain.enums.UnanswerableReason;

import java.util.List;

/** The final, verified answer to a chat/QA turn — what {@link ScopeGuard} produces, and all {@code MeetingChatService} ever persists or returns. */
public record GuardedAnswer(String answer, boolean unanswerable, UnanswerableReason reason, List<Citation> citations) {

    private static final String OUT_OF_SCOPE_MESSAGE = "That isn't discussed in this meeting.";

    public static GuardedAnswer outOfScope() {
        return new GuardedAnswer(OUT_OF_SCOPE_MESSAGE, true, UnanswerableReason.OUT_OF_SCOPE, List.of());
    }
}
