package com.ptkt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptkt.mapper.KpiMapper;
import com.ptkt.mapper.ScrumMapper;
import com.ptkt.mapper.TaskMapper;

import com.ptkt.model.Kpi;
import com.ptkt.model.Scrum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final SimpleVectorStore vectorStore;
    private final ScrumMapper scrumMapper;
    private final KpiMapper kpiMapper;
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 특정 사용자의 데이터를 벡터 스토어에 동기화
    public void syncUserData(Long userId) {
        log.info("벡터 스토어 동기화 시작: userId={}", userId);

        List<Document> documents = new ArrayList<>();

        // 1. 최근 30일 데일리 스크럼
        documents.addAll(buildScrumDocs(userId));

        // 2. 개인 KPI
        documents.addAll(buildKpiDocs(userId));

        // 3. 담당 태스크
        documents.addAll(buildTaskDocs(userId));

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            log.info("벡터 스토어 동기화 완료: userId={}, 문서={}개", userId, documents.size());
        }
    }

    private List<Document> buildScrumDocs(Long userId) {
        List<Document> docs = new ArrayList<>();
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            List<Scrum> scrums = scrumMapper.findByUserAndDateRange(userId, start, end);

            for (Scrum s : scrums) {
                StringBuilder sb = new StringBuilder();
                sb.append("[데일리 스크럼] 날짜: ").append(s.getScrumDate()).append("\n");
                if (s.getFocus() != null) sb.append("집중 목표: ").append(s.getFocus()).append("\n");
                if (s.getTasksJson() != null) {
                    try {
                        List<Map<String, Object>> tasks = objectMapper.readValue(
                                s.getTasksJson(), new TypeReference<>() {});
                        for (Map<String, Object> t : tasks) {
                            sb.append("- ").append(t.getOrDefault("title", "")).append(
                                    Boolean.TRUE.equals(t.get("done")) ? " [완료]" : " [진행중]").append("\n");
                            if (t.get("star") != null) sb.append("  ").append(t.get("star")).append("\n");
                        }
                    } catch (Exception ignored) {}
                }
                if (s.getBlocker() != null && !s.getBlocker().isBlank()) {
                    sb.append("블로커: ").append(s.getBlocker()).append("\n");
                }

                Map<String, Object> meta = Map.of(
                        "type", "scrum", "userId", userId, "date", s.getScrumDate().toString());
                docs.add(new Document(sb.toString(), meta));
            }
        } catch (Exception e) {
            log.warn("스크럼 벡터화 실패: {}", e.getMessage());
        }
        return docs;
    }

    private List<Document> buildKpiDocs(Long userId) {
        List<Document> docs = new ArrayList<>();
        try {
            List<Kpi> kpis = kpiMapper.findByOwnerId(userId);
            for (Kpi k : kpis) {
                String text = "[KPI] " + k.getName() +
                        " | 목표: " + k.getTargetValue() +
                        " | 상태: " + k.getStatus() +
                        (k.getDescription() != null ? " | " + k.getDescription() : "");
                Map<String, Object> meta = Map.of(
                        "type", "kpi", "userId", userId, "kpiId", k.getId());
                docs.add(new Document(text, meta));
            }
        } catch (Exception e) {
            log.warn("KPI 벡터화 실패: {}", e.getMessage());
        }
        return docs;
    }

    private List<Document> buildTaskDocs(Long userId) {
        List<Document> docs = new ArrayList<>();
        try {
            List<com.ptkt.model.Task> tasks = taskMapper.findByAssigneeId(userId);
            for (com.ptkt.model.Task t : tasks) {
                String text = "[태스크] " + t.getTitle() +
                        " | 상태: " + t.getStatus() +
                        " | 우선순위: " + t.getPriority() +
                        (t.getDescription() != null ? " | " + t.getDescription() : "");
                Map<String, Object> meta = Map.of(
                        "type", "task", "userId", userId, "taskId", t.getId());
                docs.add(new Document(text, meta));
            }
        } catch (Exception e) {
            log.warn("태스크 벡터화 실패: {}", e.getMessage());
        }
        return docs;
    }

    public List<Document> search(String query, int topK) {
        return vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );
    }
}
