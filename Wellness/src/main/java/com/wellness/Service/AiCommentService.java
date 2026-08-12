package com.wellness.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.wellness.Entity.QuestionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiCommentService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public AiCommentService(
            ObjectMapper objectMapper,
            @Value("${openai.enabled:false}") boolean enabled,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-5.6-sol}") String model,
            @Value("${openai.api-url:https://api.openai.com/v1/responses}") String apiUrl
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public AiCommentResult createComment(
            Map<QuestionCode, String> answers,
            Integer energyScore,
            String fallback
    ) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return new AiCommentResult(fallback, "RULE_BASED");
        }

        try {
            Map<String, Object> inputData = new LinkedHashMap<>();
            inputData.put("energyScore", energyScore);
            inputData.put("answeredQuestions", answers);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "instructions", "당신은 웰니스 코치입니다. 의료 진단이나 치료를 단정하지 마세요. " +
                            "사용자가 답한 항목만 근거로 따뜻하고 구체적인 한국어 한 문장을 60자 이내로 작성하세요. " +
                            "점수나 설문 내용을 나열하지 말고, 오늘 가장 도움이 될 행동 하나를 자연스럽게 제안하세요.",
                    "input", objectMapper.writeValueAsString(inputData),
                    "max_output_tokens", 100
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new AiCommentResult(fallback, "RULE_BASED_FALLBACK");
            }

            String comment = extractOutputText(objectMapper.readTree(response.body()));
            if (comment == null || comment.isBlank()) {
                return new AiCommentResult(fallback, "RULE_BASED_FALLBACK");
            }
            return new AiCommentResult(normalize(comment), "OPENAI");
        } catch (Exception ignored) {
            return new AiCommentResult(fallback, "RULE_BASED_FALLBACK");
        }
    }

    private String extractOutputText(JsonNode root) {
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText(null);
                }
            }
        }
        return null;
    }

    private String normalize(String value) {
        String normalized = value.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100).trim();
    }

    public record AiCommentResult(String comment, String generatedBy) {
    }
}
