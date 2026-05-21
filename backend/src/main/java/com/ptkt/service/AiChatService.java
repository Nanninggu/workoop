package com.ptkt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptkt.mapper.*;
import com.ptkt.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ScrumMapper         scrumMapper;
    private final KpiMapper            kpiMapper;
    private final KpiRecordMapper      kpiRecordMapper;
    private final TaskMapper           taskMapper;
    private final StarNoteMapper       starNoteMapper;
    private final ReviewMapper         reviewMapper;
    private final ProjectMapper        projectMapper;
    private final MembershipMapper     membershipMapper;
    private final AiChatHistoryMapper  aiChatHistoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 대화 맥락으로 포함할 이전 Q&A 쌍 수
    private static final int HISTORY_TURNS = 10;

    // ════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ════════════════════════════════════════════════════════════════════

    public String ask(Long userId, String question) {
        saveHistory(userId, "user", question);

        // ① 사용자 orgId 조회
        Long orgId = resolveOrgId(userId);

        // ② 질문 의도 → 카테고리 감지
        Set<String> categories = detectCategories(question);
        log.info("[AI] userId={} categories={} q={}", userId, categories, question);

        // ③ 카테고리별 선택적 컨텍스트 빌드
        String context = buildContext(userId, orgId, categories);

        // ④ 이전 대화 맥락 빌드
        String history = buildConversationHistory(userId);

        // ⑤ 프롬프트 조립 & AI 호출
        String prompt = assemblePrompt(context, history, question);

        String answer;
        try {
            answer = chatClientBuilder.build()
                    .prompt(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            return handleAiError(userId, e);
        }

        saveHistory(userId, "assistant", answer);
        return answer;
    }

    public List<AiChatHistory> getHistory(Long userId, int limit) {
        List<AiChatHistory> history = aiChatHistoryMapper.findRecentByUserId(userId, limit);
        Collections.reverse(history);
        return history;
    }

    // ════════════════════════════════════════════════════════════════════
    //  ① 키워드 기반 카테고리 감지
    // ════════════════════════════════════════════════════════════════════

    private Set<String> detectCategories(String question) {
        String q = question.toLowerCase();
        Set<String> cats = new LinkedHashSet<>();

        if (has(q, "kpi", "달성", "목표", "지표", "성과", "달성률", "기록", "측정", "실적"))
            cats.add("KPI");
        if (has(q, "스크럼", "일정", "블로커", "오늘", "어제", "내일", "집중", "체크인"))
            cats.add("SCRUM");
        if (has(q, "태스크", "칸반", "업무", "할일", "할 일", "과제", "작업", "완료", "미완료", "진행"))
            cats.add("TASK");
        if (has(q, "star", "스타", "경험", "스토리", "사례", "리더십", "노트"))
            cats.add("STAR");
        if (has(q, "회고", "리뷰", "주간", "월간", "지난주", "이번주", "반성", "개선", "아쉬"))
            cats.add("REVIEW");
        if (has(q, "프로젝트", "project"))
            cats.add("PROJECT");
        if (has(q, "팀", "멤버", "누가", "부하", "워크로드", "기여", "팀원", "동료", "바빠", "바쁜"))
            cats.add("TEAM");

        // 감지 실패 시 기본 세트 (스크럼 + KPI + 태스크)
        if (cats.isEmpty()) {
            cats.addAll(List.of("SCRUM", "KPI", "TASK"));
        }
        return cats;
    }

    private boolean has(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════
    //  ② 카테고리별 선택적 컨텍스트 빌드
    // ════════════════════════════════════════════════════════════════════

    private String buildContext(Long userId, Long orgId, Set<String> categories) {
        StringBuilder sb = new StringBuilder();
        if (categories.contains("SCRUM"))   appendScrum(userId, sb);
        if (categories.contains("KPI"))     appendKpi(userId, sb);
        if (categories.contains("TASK"))    appendTask(userId, sb);
        if (categories.contains("STAR"))    appendStar(userId, sb);
        if (categories.contains("REVIEW"))  appendReview(userId, sb);
        if (categories.contains("PROJECT") && orgId != null) appendProject(orgId, sb);
        if (categories.contains("TEAM")    && orgId != null) appendTeam(orgId, sb);
        return sb.toString();
    }

    private void appendScrum(Long userId, StringBuilder sb) {
        try {
            List<Scrum> list = scrumMapper.findByUserAndDateRange(
                    userId, LocalDate.now().minusDays(14), LocalDate.now());
            if (list.isEmpty()) return;
            sb.append("=== 데일리 스크럼 (최근 14일) ===\n");
            for (Scrum s : list) {
                sb.append("날짜: ").append(s.getScrumDate()).append("\n");
                if (notBlank(s.getFocus()))
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
                if (notBlank(s.getBlocker()))
                    sb.append("  블로커: ").append(s.getBlocker()).append("\n");
            }
        } catch (Exception e) { log.warn("[AI-CTX] 스크럼: {}", e.getMessage()); }
    }

    private void appendKpi(Long userId, StringBuilder sb) {
        try {
            List<Kpi> kpis = kpiMapper.findByOwnerId(userId);
            if (kpis.isEmpty()) return;
            LocalDate start = LocalDate.now().withDayOfMonth(1);
            LocalDate today = LocalDate.now();
            sb.append("\n=== KPI 현황 (이번달 기준) ===\n");
            for (Kpi k : kpis) {
                sb.append("- ").append(k.getName())
                  .append(" | 목표: ").append(k.getTargetValue())
                  .append(notBlank(k.getUnit()) ? " " + k.getUnit() : "")
                  .append(" | 상태: ").append(k.getStatus());
                try {
                    List<KpiRecord> recs = kpiRecordMapper.findByKpiIdAndDateRange(k.getId(), start, today);
                    if (!recs.isEmpty()) {
                        KpiRecord latest = recs.get(recs.size() - 1);
                        if ("BOOLEAN".equals(k.getKpiType())) {
                            sb.append(" | 달성여부: ")
                              .append(Boolean.TRUE.equals(latest.getBooleanValue()) ? "완료" : "미완료");
                        } else if (latest.getActualValue() != null && k.getTargetValue() != null) {
                            BigDecimal rate = latest.getActualValue()
                                    .divide(k.getTargetValue(), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(1, RoundingMode.HALF_UP);
                            sb.append(" | 현재값: ").append(latest.getActualValue())
                              .append(notBlank(k.getUnit()) ? " " + k.getUnit() : "")
                              .append(" | 달성률: ").append(rate).append("%");
                        }
                        sb.append(" | 이번달 기록: ").append(recs.size()).append("건");
                    } else {
                        sb.append(" | 이번달 기록 없음");
                    }
                } catch (Exception ignored) {}
                sb.append("\n");
            }
        } catch (Exception e) { log.warn("[AI-CTX] KPI: {}", e.getMessage()); }
    }

    private void appendTask(Long userId, StringBuilder sb) {
        try {
            List<Task> tasks = taskMapper.findByAssigneeId(userId);
            if (tasks.isEmpty()) return;
            sb.append("\n=== 담당 태스크 ===\n");
            tasks.stream()
                 .collect(Collectors.groupingBy(Task::getStatus))
                 .forEach((status, list) -> {
                     sb.append("[").append(status).append("]\n");
                     list.forEach(t ->
                         sb.append("  - ").append(t.getTitle())
                           .append(" | 우선순위: ").append(t.getPriority())
                           .append(t.getDueDate() != null ? " | 마감: " + t.getDueDate() : "")
                           .append(notBlank(t.getKpiName()) ? " | KPI: " + t.getKpiName() : "")
                           .append("\n"));
                 });
        } catch (Exception e) { log.warn("[AI-CTX] 태스크: {}", e.getMessage()); }
    }

    private void appendStar(Long userId, StringBuilder sb) {
        try {
            List<StarNote> notes = starNoteMapper.findByUser(userId);
            if (notes.isEmpty()) return;
            sb.append("\n=== STAR 노트 (최근 5개) ===\n");
            notes.stream().limit(5).forEach(n -> {
                sb.append("- 제목: ").append(n.getTitle()).append("\n");
                if (notBlank(n.getLpTag()))     sb.append("  태그: ").append(n.getLpTag()).append("\n");
                if (notBlank(n.getSituation())) sb.append("  상황: ").append(n.getSituation()).append("\n");
                if (notBlank(n.getTask()))      sb.append("  과제: ").append(n.getTask()).append("\n");
                if (notBlank(n.getAction()))    sb.append("  행동: ").append(n.getAction()).append("\n");
                if (notBlank(n.getResult()))    sb.append("  결과: ").append(n.getResult()).append("\n");
            });
        } catch (Exception e) { log.warn("[AI-CTX] STAR: {}", e.getMessage()); }
    }

    private void appendReview(Long userId, StringBuilder sb) {
        try {
            List<Review> reviews = reviewMapper.findByUser(userId);
            if (reviews.isEmpty()) return;
            sb.append("\n=== 회고 기록 (최근 5개) ===\n");
            reviews.stream().limit(5).forEach(r -> {
                sb.append("- [").append(r.getType()).append("] ").append(r.getPeriod()).append("\n");
                if (notBlank(r.getPlans()))          sb.append("  계획: ").append(r.getPlans()).append("\n");
                if (notBlank(r.getProgress()))        sb.append("  진행: ").append(r.getProgress()).append("\n");
                if (notBlank(r.getBestAchievement())) sb.append("  최고성과: ").append(r.getBestAchievement()).append("\n");
                if (notBlank(r.getRegrets()))         sb.append("  아쉬운점: ").append(r.getRegrets()).append("\n");
                if (r.getSelfScore() != null)         sb.append("  자기점수: ").append(r.getSelfScore()).append("/5\n");
            });
        } catch (Exception e) { log.warn("[AI-CTX] 회고: {}", e.getMessage()); }
    }

    private void appendProject(Long orgId, StringBuilder sb) {
        try {
            List<Project> projects = projectMapper.findByOrgId(orgId);
            if (projects.isEmpty()) return;
            sb.append("\n=== 프로젝트 현황 ===\n");
            projects.forEach(p ->
                sb.append("- ").append(p.getName())
                  .append(" | 태스크: ").append(p.getTaskCount()).append("개")
                  .append(notBlank(p.getDescription()) ? " | " + p.getDescription() : "")
                  .append("\n"));
        } catch (Exception e) { log.warn("[AI-CTX] 프로젝트: {}", e.getMessage()); }
    }

    private void appendTeam(Long orgId, StringBuilder sb) {
        try {
            List<Map<String, Object>> workload = taskMapper.getMemberWorkloadByOrgId(orgId);
            if (workload.isEmpty()) return;
            sb.append("\n=== 팀 워크로드 ===\n");
            workload.forEach(m ->
                sb.append("- ").append(mapVal(m, "USERNAME", "userName"))
                  .append(" | 전체: ").append(mapVal(m, "TOTALTASKS", "totalTasks")).append("개")
                  .append(" | 완료: ").append(mapVal(m, "DONETASKS", "doneTasks")).append("개")
                  .append(" | 지연: ").append(mapVal(m, "OVERDUETASKS", "overdueTasks")).append("개")
                  .append(" | 남은시간: ").append(mapVal(m, "PENDINGHOURS", "pendingHours")).append("h")
                  .append("\n"));
        } catch (Exception e) { log.warn("[AI-CTX] 팀워크로드: {}", e.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════════════
    //  ③ 대화 맥락 빌드 (이전 N턴)
    // ════════════════════════════════════════════════════════════════════

    private String buildConversationHistory(Long userId) {
        try {
            List<AiChatHistory> recent =
                    aiChatHistoryMapper.findRecentByUserId(userId, HISTORY_TURNS * 2);
            if (recent.isEmpty()) return "";
            Collections.reverse(recent);
            StringBuilder sb = new StringBuilder();
            recent.forEach(h -> {
                String role = "user".equals(h.getRole()) ? "사용자" : "AI";
                sb.append("[").append(role).append("] ").append(h.getContent()).append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.warn("[AI] 대화맥락 조회 실패: {}", e.getMessage());
            return "";
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  ④ 프롬프트 조립
    // ════════════════════════════════════════════════════════════════════

    private String assemblePrompt(String context, String history, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                당신은 팀 협업 플랫폼 CoopWork의 AI 어시스턴트입니다.
                아래 [사용자 데이터]를 바탕으로 질문에 친절하고 명확하게 답변해주세요.
                데이터에 없는 내용은 추측하지 말고 "해당 데이터가 없습니다"라고 답해주세요.
                답변은 한국어로, 핵심만 간결하게 해주세요.
                """);
        if (!context.isBlank())
            sb.append("\n[사용자 데이터]\n").append(context);
        if (!history.isBlank())
            sb.append("\n[이전 대화 맥락]\n").append(history);
        sb.append("\n[질문]\n").append(question);
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════
    //  공통 유틸
    // ════════════════════════════════════════════════════════════════════

    private Long resolveOrgId(Long userId) {
        try {
            List<Membership> list = membershipMapper.findByUserId(userId);
            return list.isEmpty() ? null : list.get(0).getOrgId();
        } catch (Exception e) {
            log.warn("[AI] orgId 조회 실패: {}", e.getMessage());
            return null;
        }
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
            log.warn("[AI] 히스토리 저장 실패: {}", e.getMessage());
        }
    }

    private String handleAiError(Long userId, Exception e) {
        log.warn("[AI] Groq 호출 실패: userId={}, error={}", userId, e.getMessage());
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        String fallback;
        if (msg.contains("429") || msg.contains("rate") || msg.contains("limit")) {
            fallback = "AI 사용량이 많아 잠시 후 다시 시도해주세요. (일시적 요청 제한)";
        } else if (msg.contains("timeout") || msg.contains("timed out")) {
            fallback = "AI 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.";
        } else {
            fallback = "AI 응답 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
        saveHistory(userId, "assistant", fallback);
        return fallback;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** H2 는 alias 를 대문자로 반환할 수 있으므로 두 키를 모두 시도 */
    private Object mapVal(Map<String, Object> m, String upper, String camel) {
        Object v = m.get(upper);
        return v != null ? v : m.getOrDefault(camel, "-");
    }
}
