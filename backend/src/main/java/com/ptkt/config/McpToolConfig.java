package com.ptkt.config;

import com.ptkt.service.OntologyService;
import com.ptkt.service.SlackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Configuration
public class McpToolConfig {

    // 동일 메시지 10초 내 중복 발송 방지
    private final ConcurrentHashMap<String, Long> recentSent = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 10_000;

    // ── Slack ──────────────────────────────────────────────────────────────

    @Bean
    @Description("Slack 채널에 메시지를 전송합니다. 팀원에게 업무 알림, 공지, 요약 내용을 보낼 때 사용하세요. 성공하면 재호출하지 마세요.")
    public Function<SlackSendRequest, String> sendSlackMessage(SlackService slackService) {
        return req -> {
            String key = req.message().substring(0, Math.min(req.message().length(), 80));
            long now = System.currentTimeMillis();
            Long last = recentSent.get(key);
            if (last != null && now - last < DEDUP_WINDOW_MS) {
                log.info("[Tool] sendSlackMessage 중복 차단: {}", key);
                return "이미 전송 완료된 메시지입니다. 재호출 불필요.";
            }
            recentSent.put(key, now);
            log.info("[Tool] sendSlackMessage: {}", req.message());
            return slackService.sendMessage(req.message());
        };
    }

    // ── DB Query ───────────────────────────────────────────────────────────

    @Bean
    @Description("""
            CoopWork 데이터베이스에서 데이터를 조회합니다. SELECT 쿼리만 허용됩니다.
            사용 가능한 테이블: task, project, kpi, kpi_record, scrum, users, membership, organization, notification, review, star_note, category, chat_message
            예시: SELECT t.title, t.status FROM task t WHERE t.status != 'DONE' LIMIT 10
            """)
    public Function<DbQueryRequest, String> queryDatabase(JdbcTemplate jdbcTemplate) {
        return req -> {
            log.info("[Tool] queryDatabase: {}", req.sql());
            return executeQuery(jdbcTemplate, req.sql());
        };
    }

    private String executeQuery(JdbcTemplate jdbcTemplate, String sql) {
        String trimmed = sql.trim().toLowerCase();

        if (!trimmed.startsWith("select")) {
            return "오류: SELECT 쿼리만 허용됩니다.";
        }
        // DML/DDL 키워드 차단
        for (String blocked : List.of("insert", "update", "delete", "drop", "truncate", "alter", "create", "merge")) {
            if (trimmed.contains(blocked)) {
                return "오류: '" + blocked + "' 는 허용되지 않습니다. SELECT만 사용 가능합니다.";
            }
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.isEmpty()) return "조회 결과 없음";

            StringBuilder sb = new StringBuilder();
            // 헤더
            rows.get(0).keySet().forEach(col -> sb.append(col).append("\t"));
            sb.append("\n");
            // 데이터 (최대 50행)
            rows.stream().limit(50).forEach(row -> {
                row.values().forEach(v -> sb.append(v != null ? v : "NULL").append("\t"));
                sb.append("\n");
            });
            if (rows.size() > 50) {
                sb.append("... (전체 ").append(rows.size()).append("행 중 50행 표시)");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[Tool] DB 쿼리 오류: {}", e.getMessage());
            return "쿼리 오류: " + e.getMessage();
        }
    }

    // ── Knowledge Graph (SPARQL) ───────────────────────────────────────────

    @Bean
    @Description("""
            CoopWork 지식 그래프(Knowledge Graph)에 SPARQL 쿼리를 실행합니다.
            User, Organization, Project, Task, KPI, ScrumRecord 클래스와 그 관계를 탐색할 수 있습니다.
            Prefix cw: <http://coopwork.io/ontology#> 는 자동으로 포함됩니다.
            예시:
              SELECT ?userName ?taskTitle WHERE {
                ?task cw:assignedTo ?user .
                ?task cw:hasStatus "IN_PROGRESS" .
                ?user cw:hasName ?userName .
                ?task cw:hasTitle ?taskTitle .
              }
            SELECT 쿼리만 허용됩니다. 결과가 없으면 빈 배열을 반환합니다.
            """)
    public Function<SparqlRequest, String> queryKnowledgeGraph(OntologyService ontologyService) {
        return req -> {
            String sparql = req.sparql();
            String lower = sparql.trim().toLowerCase();
            if (!lower.startsWith("select") && !lower.startsWith("ask")) {
                return "오류: SELECT 또는 ASK 쿼리만 허용됩니다.";
            }
            log.info("[Tool] queryKnowledgeGraph: {}", sparql.replace("\n", " "));
            try {
                var results = ontologyService.sparqlSelect(sparql);
                if (results.isEmpty()) return "쿼리 결과 없음";
                StringBuilder sb = new StringBuilder();
                results.get(0).keySet().forEach(k -> sb.append(k).append("\t"));
                sb.append("\n");
                results.stream().limit(30).forEach(row -> {
                    row.values().forEach(v -> sb.append(v).append("\t"));
                    sb.append("\n");
                });
                if (results.size() > 30)
                    sb.append("... (전체 ").append(results.size()).append("건 중 30건 표시)");
                return sb.toString();
            } catch (Exception e) {
                log.warn("[Tool] SPARQL 오류: {}", e.getMessage());
                return "SPARQL 오류: " + e.getMessage();
            }
        };
    }

    // ── Request Records ────────────────────────────────────────────────────

    public record SlackSendRequest(String message) {}
    public record DbQueryRequest(String sql) {}
    public record SparqlRequest(String sparql) {}
}
