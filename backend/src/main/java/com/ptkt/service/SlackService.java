package com.ptkt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SlackService {

    @Value("${slack.bot-token}")
    private String botToken;

    @Value("${slack.channel-id}")
    private String channelId;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getSlackUserIdByEmail(String email) {
        try {
            String encoded = URLEncoder.encode(email, StandardCharsets.UTF_8);
            String url = "https://slack.com/api/users.lookupByEmail?email=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + botToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("ok").asBoolean()) {
                log.warn("Slack 사용자 조회 실패 (email={}): {}", email, root.path("error").asText());
                return null;
            }

            String slackUserId = root.path("user").path("id").asText(null);
            log.info("Slack 사용자 매칭: email={} → slackUserId={}", email, slackUserId);
            return slackUserId;

        } catch (Exception e) {
            log.error("Slack 사용자 조회 오류", e);
            return null;
        }
    }

    public List<String> fetchMessagesSince(long oldestEpochSec, String slackUserId) {
        try {
            long latest = Instant.now().getEpochSecond();

            String url = "https://slack.com/api/conversations.history"
                    + "?channel=" + channelId
                    + "&oldest=" + oldestEpochSec
                    + "&latest=" + latest
                    + "&limit=100";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + botToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("ok").asBoolean()) {
                log.error("Slack API error: {}", root.path("error").asText());
                return List.of();
            }

            List<String> messages = new ArrayList<>();
            for (JsonNode msg : root.path("messages")) {
                // slackUserId가 있으면 해당 사용자 메시지만 필터링
                if (slackUserId != null) {
                    String msgUser = msg.path("user").asText("");
                    if (!slackUserId.equals(msgUser)) continue;
                }
                String text = msg.path("text").asText("").trim();
                if (!text.isEmpty() && !text.startsWith("<")) {
                    messages.add(text);
                }
            }

            log.info("Slack 메시지 수집: {}개 (slackUserId={}, oldest={})", messages.size(), slackUserId, oldestEpochSec);
            return messages;

        } catch (Exception e) {
            log.error("Slack 메시지 수집 실패", e);
            return List.of();
        }
    }

    public long todayStartEpoch() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }
}
