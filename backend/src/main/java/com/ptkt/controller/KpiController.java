package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.Kpi;
import com.ptkt.model.User;
import com.ptkt.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Kpi>>> getAll(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId) {
        List<Kpi> kpis;
        if (categoryId != null) {
            kpis = kpiService.findByCategoryId(user.getId(), categoryId);
        } else if ("ACTIVE".equalsIgnoreCase(status)) {
            kpis = kpiService.findActive(user.getId());
        } else {
            kpis = kpiService.findAll(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok(kpis));
    }

    @GetMapping("/team")
    public ResponseEntity<ApiResponse<List<Kpi>>> getTeamKpis(
            @RequestParam Long orgId) {
        return ResponseEntity.ok(ApiResponse.ok(kpiService.findTeamKpis(orgId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Kpi>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(kpiService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Kpi>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Kpi kpi) {
        kpi.setOwnerId(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("KPI가 생성되었습니다.", kpiService.create(kpi)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Kpi>> update(@PathVariable Long id, @RequestBody Kpi kpi) {
        return ResponseEntity.ok(ApiResponse.ok("KPI가 수정되었습니다.", kpiService.update(id, kpi)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        kpiService.updateStatus(id, body.get("status"));
        return ResponseEntity.ok(ApiResponse.ok("KPI 상태가 변경되었습니다.", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        kpiService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("KPI가 삭제되었습니다.", null));
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiResponse<Kpi>> copy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Kpi original = kpiService.findById(id);
        Kpi copy = Kpi.builder()
                .categoryId(original.getCategoryId())
                .name(original.getName() + " (복사본)")
                .description(original.getDescription())
                .unit(original.getUnit())
                .kpiType(original.getKpiType())
                .targetValue(original.getTargetValue())
                .frequency(original.getFrequency())
                .startDate(LocalDate.now())
                .endDate(original.getEndDate())
                .status("ACTIVE")
                .sortOrder(original.getSortOrder())
                .ownerId(user.getId())
                .scope("PERSONAL")
                .build();
        return ResponseEntity.ok(ApiResponse.ok("KPI가 복사되었습니다.", kpiService.create(copy)));
    }
}
