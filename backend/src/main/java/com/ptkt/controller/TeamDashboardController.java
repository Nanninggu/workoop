package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.dto.TeamDashboardDto;
import com.ptkt.mapper.KpiMapper;
import com.ptkt.mapper.KpiRecordMapper;
import com.ptkt.mapper.ScrumMapper;
import com.ptkt.mapper.TaskMapper;
import com.ptkt.model.Kpi;
import com.ptkt.model.KpiRecord;
import com.ptkt.model.Scrum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/team-dashboard")
@RequiredArgsConstructor
public class TeamDashboardController {

    private final KpiMapper       kpiMapper;
    private final KpiRecordMapper kpiRecordMapper;
    private final TaskMapper      taskMapper;
    private final ScrumMapper     scrumMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<TeamDashboardDto>> get(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "week") String period) {

        LocalDate end   = LocalDate.now();
        LocalDate start = switch (period) {
            case "month"   -> end.minusDays(29);
            case "quarter" -> end.minusDays(89);
            default        -> end.minusDays(6);
        };

        TeamDashboardDto dto = TeamDashboardDto.builder()
                .teamKpis(buildTeamKpis(orgId, end))
                .memberContributions(buildContributions(orgId, start, end))
                .velocity(buildVelocity(orgId, start, end))
                .blockers(buildBlockers(orgId, end))
                .energyMap(buildEnergyMap(orgId, start, end))
                .burnoutRisks(buildBurnoutRisks(orgId))
                .build();

        // MVP = 기여도 1위
        dto.getMemberContributions().stream()
                .filter(m -> m.getDoneTasks() > 0)
                .max(Comparator.comparingLong(TeamDashboardDto.MemberContributionDto::getDoneTasks)
                        .thenComparingDouble(TeamDashboardDto.MemberContributionDto::getKpiContribution))
                .ifPresent(dto::setMvp);

        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    // ── 팀 KPI + 달성률 ──────────────────────────────────────────────────────────

    private List<TeamDashboardDto.TeamKpiDto> buildTeamKpis(Long orgId, LocalDate today) {
        List<Kpi> kpis = kpiMapper.findTeamKpisByOrgId(orgId);
        return kpis.stream().map(kpi -> {
            LocalDate periodStart = switch (kpi.getFrequency()) {
                case "WEEKLY"  -> today.with(java.time.DayOfWeek.MONDAY);
                case "MONTHLY" -> today.withDayOfMonth(1);
                default        -> today;
            };
            Optional<KpiRecord> rec = kpiRecordMapper.findLatestByKpiIdBetween(kpi.getId(), periodStart, today);
            double current = rec.map(r -> r.getActualValue() != null
                    ? r.getActualValue().doubleValue() : 0.0).orElse(0.0);
            double target  = kpi.getTargetValue() != null ? kpi.getTargetValue().doubleValue() : 0.0;
            double rate    = target > 0 ? Math.min(current / target * 100.0, 100.0) : 0.0;

            return TeamDashboardDto.TeamKpiDto.builder()
                    .kpiId(kpi.getId())
                    .name(kpi.getName())
                    .unit(kpi.getUnit())
                    .kpiType(kpi.getKpiType())
                    .targetValue(target)
                    .currentValue(current)
                    .achievementRate(Math.round(rate * 10.0) / 10.0)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── 팀원별 기여도 ─────────────────────────────────────────────────────────────

    private List<TeamDashboardDto.MemberContributionDto> buildContributions(
            Long orgId, LocalDate start, LocalDate end) {
        return taskMapper.getMemberContributions(orgId, start, end).stream()
                .map(row -> TeamDashboardDto.MemberContributionDto.builder()
                        .userId(toLong(row.get("USERID")))
                        .userName(str(row.get("USERNAME")))
                        .totalTasks(toLong(row.get("TOTALTASKS")))
                        .doneTasks(toLong(row.get("DONETASKS")))
                        .kpiContribution(toDouble(row.get("KPICONTRIBUTION")))
                        .build())
                .collect(Collectors.toList());
    }

    // ── 일별 완료 속도 차트 ───────────────────────────────────────────────────────

    private List<TeamDashboardDto.VelocityPoint> buildVelocity(
            Long orgId, LocalDate start, LocalDate end) {
        Map<String, Long> byDate = taskMapper.getDailyVelocity(orgId, start, end).stream()
                .collect(Collectors.toMap(
                        row -> str(row.get("DONE_DATE")),
                        row -> toLong(row.get("DONE_COUNT"))));

        List<TeamDashboardDto.VelocityPoint> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            points.add(TeamDashboardDto.VelocityPoint.builder()
                    .date(d.toString())
                    .doneCount(byDate.getOrDefault(d.toString(), 0L))
                    .build());
        }
        return points;
    }

    // ── 오늘 활성 블로커 ──────────────────────────────────────────────────────────

    private List<TeamDashboardDto.BlockerDto> buildBlockers(Long orgId, LocalDate today) {
        return scrumMapper.findActiveBlockers(orgId, today).stream()
                .map(s -> TeamDashboardDto.BlockerDto.builder()
                        .userId(s.getUserId())
                        .userName(s.getUserName())
                        .blocker(s.getBlocker())
                        .severity(s.getBlockerSeverity())
                        .date(s.getScrumDate() != null ? s.getScrumDate().toString() : today.toString())
                        .build())
                .collect(Collectors.toList());
    }

    // ── 팀원별 에너지 히트맵 ──────────────────────────────────────────────────────

    private List<TeamDashboardDto.EnergyRowDto> buildEnergyMap(
            Long orgId, LocalDate start, LocalDate end) {

        List<Map<String, Object>> rows = scrumMapper.findTeamEnergyMap(orgId, start, end);

        Map<Long, TeamDashboardDto.EnergyRowDto> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long userId = toLong(row.get("USERID"));
            map.computeIfAbsent(userId, id -> TeamDashboardDto.EnergyRowDto.builder()
                    .userId(id)
                    .userName(str(row.get("USERNAME")))
                    .dates(new ArrayList<>())
                    .values(new ArrayList<>())
                    .build());
            map.get(userId).getDates().add(str(row.get("SCRUMDATE")));
            map.get(userId).getValues().add((int) toLong(row.get("ENERGY")));
        }
        return new ArrayList<>(map.values());
    }

    // ── 번아웃 위험 팀원 ──────────────────────────────────────────────────────────

    private List<TeamDashboardDto.BurnoutMemberDto> buildBurnoutRisks(Long orgId) {
        return taskMapper.getMemberWorkloadByOrgId(orgId).stream()
                .map(row -> {
                    long   overdue = toLong(row.get("OVERDUETASKS"));
                    double hours   = toDouble(row.get("PENDINGHOURS"));
                    long   total   = toLong(row.get("TOTALTASKS"));
                    long   done    = toLong(row.get("DONETASKS"));

                    List<String> signals = new ArrayList<>();
                    if (overdue >= 3) signals.add("기한 초과 " + overdue + "개");
                    if (hours  >= 30) signals.add("잔여 업무 " + String.format("%.0f", hours) + "h");
                    if (total > 0 && done == 0) signals.add("완료 태스크 없음");

                    String risk = signals.isEmpty() ? "SAFE"
                                : signals.size() == 1 ? "CAUTION" : "RISK";

                    return TeamDashboardDto.BurnoutMemberDto.builder()
                            .userId(toLong(row.get("USERID")))
                            .userName(str(row.get("USERNAME")))
                            .riskLevel(risk)
                            .signals(signals)
                            .build();
                })
                .filter(m -> !"SAFE".equals(m.getRiskLevel()))
                .collect(Collectors.toList());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────────

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

    private String str(Object v) {
        return v == null ? "" : v.toString();
    }
}
