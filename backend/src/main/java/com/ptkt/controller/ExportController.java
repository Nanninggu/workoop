package com.ptkt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptkt.dto.ApiResponse;
import com.ptkt.mapper.CategoryMapper;
import com.ptkt.mapper.KpiMapper;
import com.ptkt.mapper.KpiRecordMapper;
import com.ptkt.model.Category;
import com.ptkt.model.Kpi;
import com.ptkt.model.KpiRecord;
import com.ptkt.model.User;
import com.ptkt.service.CategoryService;
import com.ptkt.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final KpiService kpiService;
    private final CategoryService categoryService;
    private final KpiRecordMapper kpiRecordMapper;
    private final KpiMapper kpiMapper;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null)   endDate   = LocalDate.now();

        List<Kpi> kpis = kpiService.findAll(user.getId());
        if (kpis.isEmpty()) {
            return ResponseEntity.ok(new byte[0]);
        }

        List<Long> kpiIds = kpis.stream().map(Kpi::getId).collect(Collectors.toList());
        List<KpiRecord> records = kpiRecordMapper.findByKpiIdsAndDateRange(kpiIds, startDate, endDate);
        Map<Long, Kpi> kpiMap = kpis.stream().collect(Collectors.toMap(Kpi::getId, k -> k));

        StringBuilder csv = new StringBuilder();
        csv.append("날짜,카테고리,KPI명,주기,타입,목표값,단위,실적값,달성여부,달성률(%),메모\n");

        for (KpiRecord r : records) {
            Kpi kpi = kpiMap.get(r.getKpiId());
            if (kpi == null) continue;

            boolean isBoolean = "BOOLEAN".equals(kpi.getKpiType());
            String actualStr  = isBoolean
                    ? (Boolean.TRUE.equals(r.getBooleanValue()) ? "달성" : "미달성")
                    : (r.getActualValue() != null ? r.getActualValue().toPlainString() : "");
            String booleanStr = isBoolean ? actualStr : "";

            double rate = 0;
            if (isBoolean) {
                rate = Boolean.TRUE.equals(r.getBooleanValue()) ? 100 : 0;
            } else if (r.getActualValue() != null && kpi.getTargetValue() != null
                       && kpi.getTargetValue().compareTo(BigDecimal.ZERO) != 0) {
                rate = r.getActualValue().doubleValue() / kpi.getTargetValue().doubleValue() * 100;
                rate = Math.min(Math.round(rate * 10.0) / 10.0, 100.0);
            }

            String note = r.getNote() != null
                    ? "\"" + r.getNote().replace("\"", "\"\"") + "\"" : "";
            String catName = kpi.getCategory() != null ? kpi.getCategory().getName() : "";

            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%.1f,%s\n",
                    r.getRecordedDate(), escapeCsv(catName), escapeCsv(kpi.getName()),
                    kpi.getFrequency(), kpi.getKpiType(),
                    kpi.getTargetValue() != null ? kpi.getTargetValue().toPlainString() : "",
                    kpi.getUnit() != null ? kpi.getUnit() : "",
                    actualStr, booleanStr, rate, note));
        }

        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom     = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result  = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);

        String filename = "workoop_export_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + filename)
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(result);
    }

    @GetMapping("/json")
    public ResponseEntity<byte[]> exportJson(@AuthenticationPrincipal User user) throws Exception {
        List<Category> categories = categoryService.findByUser(user.getId());
        List<Kpi> kpis = kpiService.findAll(user.getId());
        List<Long> kpiIds = kpis.stream().map(Kpi::getId).collect(Collectors.toList());
        List<KpiRecord> records = kpiIds.isEmpty()
                ? List.of()
                : kpiRecordMapper.findByKpiIdsAndDateRange(kpiIds,
                    LocalDate.now().minusYears(5), LocalDate.now());

        Map<String, Object> backup = new HashMap<>();
        backup.put("version", "2.0");
        backup.put("exportedAt", LocalDate.now().toString());
        backup.put("categories", categories);
        backup.put("kpis", kpis);
        backup.put("records", records);

        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(backup);
        String filename = "workoop_backup_" + LocalDate.now() + ".json";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + filename)
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(json);
    }

    @PostMapping("/json/import")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importJson(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> backup) throws Exception {

        kpiRecordMapper.deleteAll();
        kpiMapper.deleteAll();
        categoryMapper.deleteAll();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cats = (List<Map<String, Object>>) backup.get("categories");
        int catCount = 0;
        if (cats != null) {
            for (Map<String, Object> c : cats) {
                Category cat = objectMapper.convertValue(c, Category.class);
                cat.setOwnerId(user.getId());
                categoryMapper.insertWithId(cat);
                catCount++;
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kpis = (List<Map<String, Object>>) backup.get("kpis");
        int kpiCount = 0;
        if (kpis != null) {
            for (Map<String, Object> k : kpis) {
                Kpi kpi = objectMapper.convertValue(k, Kpi.class);
                kpi.setCategory(null);
                kpi.setOwnerId(user.getId());
                kpiMapper.insertWithId(kpi);
                kpiCount++;
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) backup.get("records");
        int recCount = 0;
        if (records != null) {
            for (Map<String, Object> r : records) {
                KpiRecord rec = objectMapper.convertValue(r, KpiRecord.class);
                kpiRecordMapper.insertWithId(rec);
                recCount++;
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("복원이 완료되었습니다.",
                Map.of("categories", catCount, "kpis", kpiCount, "records", recCount)));
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
