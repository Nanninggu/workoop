package com.ptkt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ptkt.mapper.ScrumMapper;
import com.ptkt.mapper.UserMapper;
import com.ptkt.model.Scrum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SlackService slackService;
    private final ScrumMapper scrumMapper;
    private final UserMapper userMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // userId → 마지막 동기화 epoch seconds
    private final ConcurrentHashMap<Long, Long> lastSyncMap = new ConcurrentHashMap<>();

    public SyncResult syncSlack(Long userId, Long orgId) {
        // CoopWork 이메일로 Slack 사용자 ID 조회
        String slackUserId = userMapper.findById(userId)
                .map(u -> slackService.getSlackUserIdByEmail(u.getEmail()))
                .orElse(null);

        if (slackUserId == null) {
            log.warn("Slack 사용자 매칭 실패: CoopWork userId={}", userId);
        }

        // DB에 저장된 마지막 동기화 시각 우선 사용 (서버 재시작해도 유지)
        long oldest = scrumMapper.findByUserAndDate(userId, LocalDate.now())
                .map(Scrum::getLastSlackSyncAt)
                .map(ldt -> ldt.atZone(ZoneId.systemDefault()).toEpochSecond())
                .orElseGet(() -> lastSyncMap.getOrDefault(userId, slackService.todayStartEpoch()));

        List<String> messages = slackService.fetchMessagesSince(oldest, slackUserId);

        if (messages.isEmpty()) {
            return new SyncResult(false, "새로운 Slack 메시지가 없습니다.", null);
        }

        String newTasksJson = convertToTasksJson(messages);
        if (newTasksJson == null) {
            return new SyncResult(false, "AI 변환에 실패했습니다.", null);
        }

        String mergedTasksJson = mergeTasks(userId, newTasksJson);

        Scrum scrum = buildScrum(userId, orgId, mergedTasksJson);
        scrumMapper.upsert(scrum);

        lastSyncMap.put(userId, Instant.now().getEpochSecond());

        log.info("Slack 증분 동기화 완료: userId={}, 새 메시지={}개", userId, messages.size());
        return new SyncResult(true, "Slack 동기화 완료 (새 메시지 " + messages.size() + "개)", mergedTasksJson);
    }

    private String mergeTasks(Long userId, String newTasksJson) {
        List<Map<String, Object>> existing = new ArrayList<>();
        scrumMapper.findByUserAndDate(userId, LocalDate.now()).ifPresent(scrum -> {
            if (scrum.getTasksJson() != null) {
                try {
                    existing.addAll(objectMapper.readValue(scrum.getTasksJson(),
                            new TypeReference<List<Map<String, Object>>>() {}));
                } catch (Exception ignored) {}
            }
        });

        List<Map<String, Object>> newTasks = new ArrayList<>();
        try {
            newTasks.addAll(objectMapper.readValue(newTasksJson,
                    new TypeReference<List<Map<String, Object>>>() {}));
        } catch (Exception e) {
            return newTasksJson;
        }

        Set<String> existingTitles = existing.stream()
                .map(t -> String.valueOf(t.getOrDefault("title", "")))
                .collect(Collectors.toSet());

        int added = 0;
        for (Map<String, Object> task : newTasks) {
            String title = String.valueOf(task.getOrDefault("title", ""));
            if (!existingTitles.contains(title)) {
                existing.add(task);
                added++;
            }
        }

        log.info("태스크 병합: 기존={}개, 신규 추가={}개", existing.size() - added, added);
        try {
            return objectMapper.writeValueAsString(existing);
        } catch (Exception e) {
            return newTasksJson;
        }
    }

    private String convertToTasksJson(List<String> messages) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String combined = String.join("\n", messages);

            String prompt = """
                    아래는 Slack에서 수집한 업무 메시지들입니다.
                    각 메시지를 분석하여 업무 항목으로 변환해주세요.

                    [Slack 메시지]
                    %s

                    위 내용을 다음 JSON 배열 형식으로만 응답해주세요. 다른 설명 없이 JSON만:
                    [
                      {"title": "업무 제목", "star": "S: 상황\\nT: 과제\\nA: 행동\\nR: 결과(예상)", "done": false},
                      ...
                    ]
                    - title: 한 줄 업무 요약
                    - star: STAR 기법으로 정리한 내용 (S/T/A/R 각각 한 문장)
                    - done: false로 설정
                    """.formatted(combined);

            String response = chatClient.prompt(prompt).call().content().trim();

            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start >= 0 && end > start) {
                response = response.substring(start, end + 1);
            }

            ArrayNode arr = (ArrayNode) objectMapper.readTree(response);
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.size(); i++) {
                ((ObjectNode) arr.get(i)).put("id", String.valueOf(now + i));
            }
            return objectMapper.writeValueAsString(arr);

        } catch (Exception e) {
            log.error("AI 변환 실패", e);
            return null;
        }
    }

    private Scrum buildScrum(Long userId, Long orgId, String tasksJson) {
        LocalDate today = LocalDate.now();
        Optional<Scrum> existing = scrumMapper.findByUserAndDate(userId, today);
        return Scrum.builder()
                .userId(userId)
                .orgId(orgId)
                .scrumDate(today)
                .tasksJson(tasksJson)
                .blocker(existing.map(Scrum::getBlocker).orElse(null))
                .blockerSeverity(existing.map(Scrum::getBlockerSeverity).orElse("LOW"))
                .energy(existing.map(Scrum::getEnergy).orElse(3))
                .focus(existing.map(Scrum::getFocus).orElse(null))
                .lastSlackSyncAt(LocalDateTime.now())
                .build();
    }

    public record SyncResult(boolean success, String message, String tasksJson) {}
}
