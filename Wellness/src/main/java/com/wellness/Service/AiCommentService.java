package com.wellness.Service;

import com.wellness.Dto.DiagnosisRecommendationResponse;
import com.wellness.Entity.QuestionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiCommentService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final EnergyBandMessagePolicy energyBandMessagePolicy;

    public AiCommentService(
            ObjectMapper objectMapper,
            @Value("${openai.enabled:false}") boolean enabled,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-5.6-sol}") String model,
            @Value("${openai.api-url:https://api.openai.com/v1/responses}") String apiUrl,
            EnergyBandMessagePolicy energyBandMessagePolicy
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
        this.energyBandMessagePolicy = energyBandMessagePolicy;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public AiScreenCommentResult createScreenComments(
            Map<QuestionCode, String> answers,
            Integer energyScore,
            String fallbackHeadline,
            String fallbackSummary,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        AiScreenCommentResult fallback = fallbackScreenComments(
                energyScore, fallbackSummary, todos, avoidances, "RULE_BASED");
        if (!isAvailable()) {
            return fallback;
        }

        try {
            Map<String, Object> inputData = new LinkedHashMap<>();
            inputData.put("energyScore", energyScore);
            inputData.put("answeredQuestions", answers);
            inputData.put("todayRecommendations", todos);
            inputData.put("notTodayRecommendations", avoidances);

            String instructions = "당신은 Not Today 웰니스 서비스의 한국어 코치입니다. "
                    + "핵심 목적은 사용자가 오늘 하지 않아도 되는 일을 발견해 부담을 덜도록 돕는 것입니다. "
                    + "친한 코치가 조용히 이야기하듯 자연스러운 존댓말(~해요, ~괜찮아요)을 사용하세요. "
                    + "광고 문구, 기계적인 나열, 과장된 위로, 훈계, 죄책감을 주는 표현은 피하세요. "
                    + "'반영했어요', '답했어요' 같은 보고서 말투를 반복하지 마세요. "
                    + "의료 진단이나 치료를 단정하지 말고, 제공되지 않은 상태나 원인을 추측하지 마세요. "
                    + "Not Today 항목은 금지 명령이 아니라 오늘 미루거나 강도를 낮춰도 된다는 허락처럼 표현하세요. "
                    + "반드시 JSON 객체만 출력하고 마크다운을 사용하지 마세요. "
                    + "키는 diagnosisComment, routeJudgmentComment, profileComment 세 개입니다. "
                    + "diagnosisComment는 에너지 상태의 근거 하나를 자연스럽게 짚고, 오늘 미뤄도 되는 행동을 먼저 제안하는 2~3문장입니다. "
                    + "routeJudgmentComment는 notTodayRecommendations에 있는 항목 중 한두 개를 실제 제목으로 언급하고, 각 reason을 근거로 왜 오늘 루트에서 걷어냈는지 설명하는 2~3문장입니다. "
                    + "Not Today 항목이 없다면 추가로 걷어낼 항목이 없었던 이유를 현재 에너지와 연결해 설명하세요. "
                    + "profileComment는 사용자를 고정된 성격으로 단정하지 않고, 최근 답변에서 보이는 관리 경향을 한 문장으로 설명하세요.";

            String output = requestText(instructions, objectMapper.writeValueAsString(inputData), 450);
            JsonNode content = objectMapper.readTree(stripCodeFence(output));
            String diagnosis = requiredText(content, "diagnosisComment");
            String routeJudgment = requiredText(content, "routeJudgmentComment");
            String profile = requiredText(content, "profileComment");

            return new AiScreenCommentResult(
                    energyBandMessagePolicy.homeMessage(energyScore),
                    normalize(diagnosis, 500),
                    energyBandMessagePolicy.routeMessage(energyScore),
                    normalize(routeJudgment, 500),
                    normalize(profile, 250),
                    "OPENAI"
            );
        } catch (Exception ignored) {
            return fallbackScreenComments(
                    energyScore, fallbackSummary, todos, avoidances, "RULE_BASED_FALLBACK");
        }
    }

    public AiCommentResult createReportInsight(Map<String, Object> reportData, String fallback) {
        if (!isAvailable()) {
            return new AiCommentResult(fallback, "RULE_BASED");
        }

        try {
            String instructions = "당신은 Not Today 웰니스 리포트 코치입니다. 의료 진단을 하지 마세요. "
                    + "친한 코치가 말하듯 자연스러운 존댓말을 사용하고 통계를 기계적으로 나열하지 마세요. "
                    + "제공된 기간 통계만 근거로 한국어 2~3문장으로 변화를 설명하세요. "
                    + "먼저 다음 기간에 줄이거나 쉬어가도 되는 행동을 제안하고, 대신 유지할 작은 행동 하나를 덧붙이세요. "
                    + "데이터가 적으면 단정하지 말고 기록이 더 필요하다고 안내하세요.";
            String output = requestText(
                    instructions, objectMapper.writeValueAsString(reportData), 220);
            return new AiCommentResult(normalize(output, 500), "OPENAI");
        } catch (Exception ignored) {
            return new AiCommentResult(fallback, "RULE_BASED_FALLBACK");
        }
    }

    public AiCommentResult createProfileInsight(Map<String, Object> history, String fallback) {
        if (!isAvailable() || ((Number) history.get("analyzedDays")).intValue() == 0) {
            return new AiCommentResult(fallback, "RULE_BASED");
        }

        try {
            String instructions = "당신은 Not Today 웰니스 서비스의 한국어 코치입니다. "
                    + "최근 진단 기록에서 반복되는 관리 경향을 바탕으로 사용자를 따뜻하게 설명하세요. "
                    + "사람의 성격이나 건강 상태를 단정하거나 의료 진단을 하지 마세요. "
                    + "부족함을 지적하지 말고 어떤 방식의 회복과 관리가 잘 맞아 보이는지 존댓말 한두 문장, 180자 이내로 작성하세요.";
            String output = requestText(
                    instructions, objectMapper.writeValueAsString(history), 160);
            return new AiCommentResult(normalize(output, 250), "OPENAI");
        } catch (Exception ignored) {
            return new AiCommentResult(fallback, "RULE_BASED_FALLBACK");
        }
    }

    private String requestText(String instructions, String input, int maxOutputTokens) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("reasoning", Map.of("effort", "low"));
        requestBody.put("text", Map.of("verbosity", "low"));
        requestBody.put("instructions", instructions);
        requestBody.put("input", input);
        requestBody.put("max_output_tokens", maxOutputTokens);

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI request failed: " + response.statusCode());
        }

        String output = extractOutputText(objectMapper.readTree(response.body()));
        if (output == null || output.isBlank()) {
            throw new IllegalStateException("OpenAI response did not contain output text");
        }
        return output;
    }

    private AiScreenCommentResult fallbackScreenComments(
            Integer energyScore,
            String summary,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances,
            String generatedBy
    ) {
        String routeJudgment = fallbackRouteJudgment(avoidances);
        return new AiScreenCommentResult(
                energyBandMessagePolicy.homeMessage(energyScore),
                summary,
                energyBandMessagePolicy.routeMessage(energyScore),
                routeJudgment,
                "최근 답변을 보면 무리해서 채우기보다 작은 회복 행동을 이어가는 방식이 잘 맞아 보여요.",
                generatedBy
        );
    }

    private String fallbackRouteJudgment(List<DiagnosisRecommendationResponse> avoidances) {
        if (avoidances.isEmpty()) {
            return "현재 에너지와 답변을 살펴보니 오늘 루트에서 추가로 걷어낼 항목은 없었어요. 그래도 한 번에 에너지를 모두 쓰지는 않아도 괜찮아요.";
        }
        DiagnosisRecommendationResponse first = avoidances.get(0);
        StringBuilder comment = new StringBuilder("오늘은 ")
                .append(first.title()).append(" 항목을 걷어냈어요. ")
                .append(first.reason());
        if (avoidances.size() > 1) {
            DiagnosisRecommendationResponse second = avoidances.get(1);
            comment.append(" 또 ").append(second.title()).append("도 미뤄두었어요. ")
                    .append(second.reason());
        }
        return normalize(comment.toString(), 500);
    }

    private boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private String extractOutputText(JsonNode root) {
        StringBuilder outputText = new StringBuilder();
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    String text = content.path("text").asText(null);
                    if (text != null) outputText.append(text);
                }
            }
        }
        return outputText.isEmpty() ? null : outputText.toString();
    }

    private String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing AI response field: " + field);
        }
        return value;
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstNewLine = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        return firstNewLine >= 0 && lastFence > firstNewLine
                ? trimmed.substring(firstNewLine + 1, lastFence).trim()
                : trimmed;
    }

    private String normalize(String value, int maxLength) {
        String normalized = value.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength).trim();
    }

    public record AiCommentResult(String comment, String generatedBy) {
    }

    public record AiScreenCommentResult(
            String homeComment,
            String diagnosisComment,
            String routeComment,
            String routeJudgmentComment,
            String profileComment,
            String generatedBy
    ) {
    }
}
