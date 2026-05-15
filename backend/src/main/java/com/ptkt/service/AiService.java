package com.ptkt.service;

import com.ptkt.mapper.KpiMapper;
import com.ptkt.mapper.MembershipMapper;
import com.ptkt.mapper.ScrumMapper;
import com.ptkt.model.Kpi;
import com.ptkt.model.Membership;
import com.ptkt.model.Scrum;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatModel chatModel;
    private final KpiMapper kpiMapper;
    private final ScrumMapper scrumMapper;
    private final MembershipMapper membershipMapper;
    private final JdbcTemplate jdbc;

    public String ask(Long orgId, String orgName, String question) {
        String context = buildContext(orgId, orgName);
        String system =
            "당신은 Workoop 팀 협업 플랫폼의 AI 어시스턴트입니다. " +
            "아래 팀 데이터를 기반으로 질문에 한국어로 답변하세요. " +
            "데이터에 없는 내용은 없다고 솔직하게 말하세요. " +
            "숫자나 현황을 언급할 때는 구체적으로 인용하세요.\n\n" +
            context;

        Prompt prompt = new Prompt(List.of(
            new SystemMessage(system),
            new UserMessage(question)
        ));

        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    private String buildContext(Long orgId, String orgName) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 조직: ").append(orgName).append(" ===\n\n");

        // 팀 멤버
        List<Membership> members = membershipMapper.findByOrgId(orgId);
        sb.append("【팀 멤버】\n");
        members.forEach(m -> sb.append("- ").append(m.getUserName()).append(" (").append(m.getRole()).append(")\n"));
        sb.append("\n");

        // 태스크 현황 (org → project → task JOIN)
        List<Map<String, Object>> tasks = jdbc.queryForList(
            "SELECT t.title, t.status, t.priority, u.name AS assignee, t.due_date " +
            "FROM task t " +
            "LEFT JOIN project p ON t.project_id = p.id " +
            "LEFT JOIN users u ON t.assignee_id = u.id " +
            "WHERE p.org_id = ? ORDER BY t.created_at DESC LIMIT 30", orgId);

        sb.append("【태스크 현황 (최근 30건)】\n");
        if (tasks.isEmpty()) {
            sb.append("- 등록된 태스크 없음\n");
        } else {
            tasks.forEach(t -> {
                sb.append("- [").append(t.get("STATUS")).append("] ")
                  .append(t.get("TITLE"))
                  .append(" | 담당: ").append(t.getOrDefault("ASSIGNEE", "미배정"))
                  .append(" | 우선순위: ").append(t.getOrDefault("PRIORITY", "-"));
                Object due = t.get("DUE_DATE");
                if (due != null) sb.append(" | 마감: ").append(due);
                sb.append("\n");
            });
        }
        sb.append("\n");

        // KPI 현황
        List<Kpi> kpis = kpiMapper.findTeamKpisByOrgId(orgId);
        sb.append("【KPI 현황】\n");
        if (kpis.isEmpty()) {
            sb.append("- 등록된 KPI 없음\n");
        } else {
            kpis.forEach(k -> {
                sb.append("- ").append(k.getName())
                  .append(" | 유형: ").append(k.getKpiType())
                  .append(" | 상태: ").append(k.getStatus())
                  .append(" | 주기: ").append(k.getFrequency())
                  .append("\n");
            });
        }
        sb.append("\n");

        // 오늘 스크럼
        LocalDate today = LocalDate.now();
        List<Scrum> scrums = scrumMapper.findTeamByDate(orgId, today);
        sb.append("【오늘 스크럼 (").append(today.format(DateTimeFormatter.ofPattern("MM월 dd일"))).append(")】\n");
        if (scrums.isEmpty()) {
            sb.append("- 오늘 작성된 스크럼 없음\n");
        } else {
            scrums.forEach(s -> {
                sb.append("- ").append(s.getUserName()).append("\n");
                if (s.getTasksJson() != null && !s.getTasksJson().isBlank())
                    sb.append("  오늘 할 일: ").append(s.getTasksJson()).append("\n");
                if (s.getBlocker() != null && !s.getBlocker().isBlank())
                    sb.append("  블로커: ").append(s.getBlocker())
                      .append(" [").append(s.getBlockerSeverity()).append("]\n");
                if (s.getEnergy() != null)
                    sb.append("  에너지: ").append(s.getEnergy()).append("/5\n");
            });
        }
        sb.append("\n");

        // 활성 블로커
        List<Scrum> blockers = scrumMapper.findActiveBlockers(orgId, today);
        if (!blockers.isEmpty()) {
            sb.append("【현재 블로커】\n");
            blockers.forEach(b ->
                sb.append("- ").append(b.getUserName()).append(": ")
                  .append(b.getBlocker())
                  .append(" [").append(b.getBlockerSeverity()).append("]\n")
            );
        }

        return sb.toString();
    }
}
