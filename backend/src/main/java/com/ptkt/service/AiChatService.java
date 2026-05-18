package com.ptkt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptkt.mapper.AiChatHistoryMapper;
import com.ptkt.mapper.KpiMapper;
import com.ptkt.mapper.ScrumMapper;
import com.ptkt.mapper.TaskMapper;
import com.ptkt.model.AiChatHistory;
import com.ptkt.model.Kpi;
import com.ptkt.model.Scrum;
import com.ptkt.model.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ScrumMapper scrumMapper;
    private final KpiMapper kpiMapper;
    private final TaskMapper taskMapper;
    private final AiChatHistoryMapper aiChatHistoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String ask(Long userId, String question) {
        saveHistory(userId, "user", question);

        String context = buildContext(userId);

        if (context.isBlank()) {
            String noData = "아직 데이터가 없습니다. 데일리 스크럼이나 KPI를 먼저 입력해주세요.";
            saveHistory(userId, "assistant", noData);
            return noData;
        }

        String prompt = """
                당신은 팀 협업 플랫폼 CoopWork의 AI 어시스턴트입니다.
                아래 [사용자 데이터]를 바탕으로 질문에 친절하고 명확하게 답변해주세요.
                데이터에 없는 내용은 추측하지 말고 "해당 데이터가 없습니다"라고 답해주세요.
                답변은 한국어로 해주세요.

                [사용자 데이터]
                %s

                [질문]
                %s
                """.formatted(context, question);

        log.info("AI 채팅 질문: userId={}, question={}", userId, question);

        String answer = chatClientBuilder.build()
                .prompt(prompt)
                .call()
                .content();

        saveHistory(userId, "assistant", answer);
        return answer;
    }

    public List<AiChatHistory> getHistory(Long userId, int limit) {
        List<AiChatHistory> history = aiChatHistoryMapper.findRecentByUserId(userId, limit);
        Collections.reverse(history);
        return history;
    }

    private void saveHistory(Long userId, String role, String content) {
        try {
            AiChatHistory h = new AiChatHistory();
            h.setUserId(userId);
            h.setRole(role);
            h.setContent(content != null && content.length() > 4000
                    ? content.substring(0, 4000) : content);
            aiChatHistoryMapper.insert(h);
        } catch (Exception e) {
            log.warn("AI 채팅 히스토리 저장 실패: {}", e.getMessage());
        }
    }

    private String buildContext(Long userId) {
        StringBuilder sb = new StringBuilder();

        // 최근 14일 스크럼
        try {
            List<Scrum> scrums = scrumMapper.findByUserAndDateRange(
                    userId, LocalDate.now().minusDays(14), LocalDate.now());
            if (!scrums.isEmpty()) {
                sb.append("=== 데일리 스크럼 (최근 14일) ===\n");
                for (Scrum s : scrums) {
                    sb.append("날짜: ").append(s.getScrumDate()).append("\n");
                    if (s.getFocus() != null && !s.getFocus().isBlank())
                        sb.append("  집중목표: ").append(s.getFocus()).append("\n");
                    if (s.getTasksJson() != null) {
                        try {
                            List<Map<String, Object>> tasks = objectMapper.readValue(
                                    s.getTasksJson(), new TypeReference<>() {});
                            for (Map<String, Object> t : tasks) {
                                boolean done = Boolean.TRUE.equals(t.get("done"));
                                sb.append("  - ").append(t.getOrDefault("title", ""))
                                        .append(done ? " [완료]" : " [미완료]").append("\n");
                            }
                        } catch (Exception ignored) {}
                    }
                    if (s.getBlocker() != null && !s.getBlocker().isBlank())
                        sb.append("  블로커: ").append(s.getBlocker()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("스크럼 조회 실패: {}", e.getMessage());
        }

        // KPI
        try {
            List<Kpi> kpis = kpiMapper.findByOwnerId(userId);
            if (!kpis.isEmpty()) {
                sb.append("\n=== KPI ===\n");
                for (Kpi k : kpis) {
                    sb.append("- ").append(k.getName())
                            .append(" | 목표: ").append(k.getTargetValue())
                            .append(" | 상태: ").append(k.getStatus()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("KPI 조회 실패: {}", e.getMessage());
        }

        // 담당 태스크
        try {
            List<Task> tasks = taskMapper.findByAssigneeId(userId);
            if (!tasks.isEmpty()) {
                sb.append("\n=== 담당 태스크 ===\n");
                for (Task t : tasks) {
                    sb.append("- ").append(t.getTitle())
                            .append(" | ").append(t.getStatus())
                            .append(" | 우선순위: ").append(t.getPriority()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("태스크 조회 실패: {}", e.getMessage());
        }

        return sb.toString();
    }
}
