package com.meetingiq.platform.llm.openai;

import com.meetingiq.platform.config.LlmProperties;
import com.meetingiq.platform.domain.TokenUsage;
import com.meetingiq.platform.llm.core.AbstractLlmProvider;
import com.meetingiq.platform.llm.core.LlmResponseCache;
import com.meetingiq.platform.llm.spi.FinishReason;
import com.meetingiq.platform.llm.spi.JsonResponseSchema;
import com.meetingiq.platform.llm.spi.LlmCompletion;
import com.meetingiq.platform.llm.spi.LlmOptions;
import com.meetingiq.platform.llm.spi.LlmQuery;
import com.meetingiq.platform.llm.spi.ProviderDescriptor;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The OpenAI implementation of {@link com.meetingiq.platform.llm.spi.LlmProvider}.
 * This is the ONLY class in the app that imports the {@code com.openai.*}
 * SDK for chat completions — everything above {@link AbstractLlmProvider}
 * (services, controllers, the whole {@code llm.task} layer) works purely
 * against the vendor-neutral {@code llm.spi} types. Adding Gemini or Claude
 * means writing a sibling class shaped exactly like this one; nothing here
 * is referenced from outside this package except through the SPI.
 */
@Component
public class OpenAiLlmProvider extends AbstractLlmProvider {

    private static final String PROVIDER_ID = "openai";

    private final LlmProperties properties;
    private final OpenAIClient client;

    public OpenAiLlmProvider(LlmProperties properties, LlmResponseCache cache, ObjectProvider<OpenAIClient> clientProvider) {
        super(cache);
        this.properties = properties;
        this.client = clientProvider.getIfAvailable();
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
                PROVIDER_ID, "OpenAI", true, true,
                properties.openai().chatModel(), properties.openai().embeddingModel()
        );
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    protected String resolveModel(LlmOptions options) {
        String requested = options == null ? null : options.model();
        return (requested != null && !requested.isBlank()) ? requested : properties.openai().chatModel();
    }

    @Override
    protected LlmCompletion doComplete(LlmQuery<?> query, String resolvedModel) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(resolvedModel)
                .addSystemMessage(query.systemPrompt())
                .addUserMessage(query.userPrompt())
                .temperature(query.options().temperature())
                .maxCompletionTokens((long) query.options().maxOutputTokens());

        if (query.options().jsonMode()) {
            JsonResponseSchema schema = query.responseSchema();
            if (schema != null) {
                builder.responseFormat(structuredOutputFormat(schema));
            } else {
                builder.responseFormat(ResponseFormatJsonObject.builder().build());
            }
        }

        ChatCompletion completion = client.chat().completions().create(builder.build());
        ChatCompletion.Choice choice = completion.choices().get(0);

        String text = choice.message().content().orElse("");
        TokenUsage usage = completion.usage()
                .map(u -> new TokenUsage((int) u.promptTokens(), (int) u.completionTokens(), (int) u.totalTokens()))
                .orElse(TokenUsage.ZERO);

        return new LlmCompletion(text, completion.model(), usage, mapFinishReason(choice.finishReason()));
    }

    /**
     * Maps a vendor-neutral {@link JsonResponseSchema} onto OpenAI's Structured Outputs
     * ({@code strict: true}) — the model's response is then guaranteed by the API itself to
     * match the schema exactly (every property present, no extras), rather than merely being
     * syntactically valid JSON as {@link ResponseFormatJsonObject} only guarantees.
     */
    private static ResponseFormatJsonSchema structuredOutputFormat(JsonResponseSchema schema) {
        Map<String, JsonValue> schemaProperties = new LinkedHashMap<>();
        schema.schema().forEach((key, value) -> schemaProperties.put(key, JsonValue.from(value)));

        ResponseFormatJsonSchema.JsonSchema.Schema jsonSchema = ResponseFormatJsonSchema.JsonSchema.Schema.builder()
                .additionalProperties(schemaProperties)
                .build();

        ResponseFormatJsonSchema.JsonSchema namedSchema = ResponseFormatJsonSchema.JsonSchema.builder()
                .name(schema.name())
                .schema(jsonSchema)
                .strict(true)
                .build();

        return ResponseFormatJsonSchema.builder().jsonSchema(namedSchema).build();
    }

    private static FinishReason mapFinishReason(ChatCompletion.Choice.FinishReason reason) {
        return switch (reason.value()) {
            case STOP -> FinishReason.STOP;
            case LENGTH -> FinishReason.LENGTH;
            case CONTENT_FILTER -> FinishReason.CONTENT_FILTER;
            // Neither tool-calling nor legacy function-calling is used by this app's prompts;
            // both indicate the model otherwise completed normally.
            case TOOL_CALLS, FUNCTION_CALL -> FinishReason.STOP;
            default -> FinishReason.UNKNOWN;
        };
    }
}
