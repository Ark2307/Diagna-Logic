package com.diagna.logic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code diagna.chat.*} from application.yml. */
@ConfigurationProperties(prefix = "diagna.chat")
public record ChatProperties(
        int historyBudgetTokens
) {
}
