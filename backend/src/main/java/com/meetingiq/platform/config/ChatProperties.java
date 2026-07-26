package com.meetingiq.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code meetingiq.chat.*} from application.yml. */
@ConfigurationProperties(prefix = "meetingiq.chat")
public record ChatProperties(
        int historyBudgetTokens
) {
}
