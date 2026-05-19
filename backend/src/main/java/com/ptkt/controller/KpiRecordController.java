package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.dto.KpiRecordRequest;
import com.ptkt.model.KpiRecord;
import com.ptkt.model.User;
import com.ptkt.service.KpiRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class KpiRecordController {

    private final KpiRecordService kpiRecordService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<KpiRecord>>> getByDate(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(kpiRecordService.findByOwnerIdAndDate(user.getId(), queryDate)));
    }

    /** 날짜 범위 내 본인 KPI 기록 조회 (캘린더/분석 뷰용) */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<KpiRecord>>> getByDateRange(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(kpiRecordService.findByOwnerIdAndDateRange(user.getId(), startDate, endDate)));
    }

    @GetMapping("/kpi/{kpiId}")
    public ResponseEntity<ApiResponse<List<KpiRecord>>> getByKpiId(
            @PathVariable Long kpiId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<KpiRecord> records;
        if (startDate != null && endDate != null) {
            records = kpiRecordService.findByKpiIdAndDateRange(kpiId, startDate, endDate);
        } else {
            records = kpiRecordService.findByKpiId(kpiId);
        }
        return ResponseEntity.ok(ApiResponse.ok(records));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KpiRecord>> upsert(@RequestBody KpiRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("기록이 저장되었습니다.", kpiRecordService.upsert(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        kpiRecordService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("기록이 삭제되었습니다.", null));
    }
}
