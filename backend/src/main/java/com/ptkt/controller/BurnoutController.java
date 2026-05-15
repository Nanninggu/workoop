package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.mapper.KpiRecordMapper;
import com.ptkt.mapper.TaskMapper;
import com.ptkt.model.KpiRecord;
import com.ptkt.model.Task;
import com.ptkt.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/burnout")
@RequiredArgsConstructor
public class BurnoutController {

    private final TaskMapper taskMapper;
    private final KpiRecordMapper kpiRecordMapper;

    // 개인 번아웃 신호 (KPI 기록 기반)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myBurnout(
            @AuthenticationPrincipal User user) {

        // 최근 14일 KPI 기록 가져오기
        LocalDate end   = LocalDate.now();
        LocalDate start = end.minusDays(13);
        List<KpiRecord> records = kpiRecordMapper.findByDateRange(start, end);

        // 내 담당 태스크 과부하 분석
        List<Task> myTasks = taskMapper.findByAssigneeId(user.getId());

        long overdue = myTasks.stream()
                .filter(t -> !"DONE".equals(t.getStatus()) && t.getDueDate() != null
                          && t.getDueDate().isBefore(LocalDate.now()))
                .count();

        double pendingHours = myTasks.stream()
                .filter(t -> !"DONE".equals(t.getStatus()))
                .mapToDouble(t -> t.getEstimatedHours() != null
                        ? t.getEstimatedHours().doubleValue() : 0)
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("overdueTasks",  overdue);
        result.put("pendingHours",  pendingHours);
        result.put("activeTasks",   myTasks.stream().filter(t -> !"DONE".equals(t.getStatus())).count());
        result.put("recordDays",    records.size());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // 팀 번아웃 현황 (관리자용)
    @GetMapping("/team")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> teamBurnout(
            @RequestParam Long orgId) {

        List<Map<String, Object>> workload = taskMapper.getMemberWorkloadByOrgId(orgId);

        List<Map<String, Object>> result = workload.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>(row);

            long overdueCount  = toLong(row.get("OVERTASKS"));
            if (overdueCount == 0) overdueCount = toLong(row.get("OVERDUETASKS"));
            double hours       = toDouble(row.get("PENDINGHOURS"));
            long   total       = toLong(row.get("TOTALTASKS"));
            long   done        = toLong(row.get("DONETASKS"));

            // 번아웃 위험도 계산
            List<String> signals = new ArrayList<>();
            if (overdueCount >= 3)  signals.add("기한 초과 태스크 " + overdueCount + "개");
            if (hours >= 30)        signals.add("잔여 예상 업무 " + String.format("%.0f", hours) + "시간");
            if (total > 0 && done == 0) signals.add("완료 태스크 없음");

            String risk = signals.isEmpty() ? "SAFE"
                        : signals.size() == 1 ? "CAUTION" : "RISK";

            m.put("riskLevel", risk);
            m.put("signals",   signals);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }
}
