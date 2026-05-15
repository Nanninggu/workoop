package com.ptkt.service;

import com.ptkt.dto.DashboardDto;
import com.ptkt.mapper.KpiRecordMapper;
import com.ptkt.model.Category;
import com.ptkt.model.Kpi;
import com.ptkt.model.KpiRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final KpiService kpiService;
    private final CategoryService categoryService;
    private final KpiRecordMapper kpiRecordMapper;

    public DashboardDto getDashboard(Long ownerId) {
        List<Kpi> allKpis = kpiService.findAll(ownerId);
        List<Kpi> activeKpis = allKpis.stream()
                .filter(k -> "ACTIVE".equals(k.getStatus()))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();

        // 🔴 버그 수정: 각 KPI 주기에 맞는 기간 내 입력 완료 수 집계
        int recordedInPeriod = countRecordedInPeriod(activeKpis, today);

        List<DashboardDto.KpiSummaryDto> kpiSummaries = buildKpiSummaries(activeKpis, today);

        double overallRate = kpiSummaries.stream()
                .mapToDouble(DashboardDto.KpiSummaryDto::getAchievementRate)
                .average()
                .orElse(0.0);

        List<Category> categories = categoryService.findAll();
        List<DashboardDto.CategorySummaryDto> categorySummaries = buildCategorySummaries(categories, kpiSummaries);

        List<DashboardDto.TrendPoint> trendPoints = buildTrendPoints(activeKpis, today);
        DashboardDto.PeriodSummary periodSummary = buildPeriodSummary(activeKpis, today);

        return DashboardDto.builder()
                .totalKpis(allKpis.size())
                .activeKpis(activeKpis.size())
                .todayRecorded(recordedInPeriod)    // 하위 호환
                .recordedInPeriod(recordedInPeriod)
                .overallAchievementRate(Math.round(overallRate * 10.0) / 10.0)
                .kpiSummaries(kpiSummaries)
                .categorySummaries(categorySummaries)
                .trendPoints(trendPoints)
                .periodSummary(periodSummary)
                .build();
    }

    // ─── KPI 요약 (주기별 올바른 기준으로 달성률 계산) ───────────────────────────

    private List<DashboardDto.KpiSummaryDto> buildKpiSummaries(List<Kpi> kpis, LocalDate today) {
        return kpis.stream().map(kpi -> {
            LocalDate periodStart = getPeriodStart(kpi.getFrequency(), today);

            // 🔴 버그 수정: 오늘 하루가 아닌 "주기 기간 내 최신 기록" 사용
            Optional<KpiRecord> periodRecord =
                    kpiRecordMapper.findLatestByKpiIdBetween(kpi.getId(), periodStart, today);

            BigDecimal latestValue   = periodRecord.map(KpiRecord::getActualValue).orElse(null);
            Boolean   latestBoolean  = periodRecord.map(KpiRecord::getBooleanValue).orElse(null);
            boolean   recordedInPeriod = periodRecord.isPresent();

            double achievementRate = calculateAchievementRate(kpi, latestValue, latestBoolean);

            // 연속 달성 일수 (DAILY KPI 전용)
            int streak = 0;
            if ("DAILY".equals(kpi.getFrequency())) {
                List<LocalDate> dates = kpiRecordMapper.findRecordedDatesByKpiId(kpi.getId());
                streak = calculateStreak(dates, today);
            }

            String periodLabel = switch (kpi.getFrequency()) {
                case "WEEKLY"  -> "이번 주";
                case "MONTHLY" -> "이번 달";
                default        -> "오늘";
            };

            return DashboardDto.KpiSummaryDto.builder()
                    .kpiId(kpi.getId())
                    .kpiName(kpi.getName())
                    .categoryName(kpi.getCategory() != null ? kpi.getCategory().getName() : "")
                    .categoryColor(kpi.getCategory() != null ? kpi.getCategory().getColor() : "#4F46E5")
                    .kpiType(kpi.getKpiType())
                    .unit(kpi.getUnit())
                    .targetValue(kpi.getTargetValue())
                    .latestValue(latestValue)
                    .latestBooleanValue(latestBoolean)
                    .achievementRate(achievementRate)
                    .frequency(kpi.getFrequency())
                    .status(kpi.getStatus())
                    .recordedToday(recordedInPeriod)       // 하위 호환
                    .recordedInPeriod(recordedInPeriod)
                    .periodLabel(periodLabel)
                    .streak(streak)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─── 달성률 계산 ─────────────────────────────────────────────────────────────

    private double calculateAchievementRate(Kpi kpi, BigDecimal actual, Boolean booleanVal) {
        if ("BOOLEAN".equals(kpi.getKpiType())) {
            return Boolean.TRUE.equals(booleanVal) ? 100.0 : 0.0;
        }
        if (actual == null || kpi.getTargetValue() == null
                || kpi.getTargetValue().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        double rate = actual.doubleValue() / kpi.getTargetValue().doubleValue() * 100.0;
        return Math.min(Math.round(rate * 10.0) / 10.0, 100.0);
    }

    // ─── 연속 달성 Streak 계산 ────────────────────────────────────────────────────

    private int calculateStreak(List<LocalDate> sortedDatesDesc, LocalDate today) {
        if (sortedDatesDesc.isEmpty()) return 0;
        LocalDate mostRecent = sortedDatesDesc.get(0);
        // 오늘 또는 어제를 기준으로 streak 시작, 그 이전이면 0
        if (!mostRecent.equals(today) && !mostRecent.equals(today.minusDays(1))) return 0;

        int streak = 0;
        LocalDate expected = mostRecent;
        for (LocalDate date : sortedDatesDesc) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break; // 날짜 공백 발생 → streak 종료
            }
        }
        return streak;
    }

    // ─── 기간 시작일 계산 ─────────────────────────────────────────────────────────

    private LocalDate getPeriodStart(String frequency, LocalDate today) {
        return switch (frequency) {
            case "WEEKLY"  -> today.with(DayOfWeek.MONDAY);
            case "MONTHLY" -> today.withDayOfMonth(1);
            default        -> today;
        };
    }

    // ─── 기간 내 입력 완료 수 집계 ────────────────────────────────────────────────

    private int countRecordedInPeriod(List<Kpi> kpis, LocalDate today) {
        return (int) kpis.stream().filter(kpi -> {
            LocalDate periodStart = getPeriodStart(kpi.getFrequency(), today);
            return kpiRecordMapper.countByPeriodAndKpiId(kpi.getId(), periodStart, today) > 0;
        }).count();
    }

    // ─── 카테고리별 달성률 요약 ────────────────────────────────────────────────────

    private List<DashboardDto.CategorySummaryDto> buildCategorySummaries(
            List<Category> categories, List<DashboardDto.KpiSummaryDto> kpiSummaries) {

        Map<String, List<DashboardDto.KpiSummaryDto>> byCategory = kpiSummaries.stream()
                .collect(Collectors.groupingBy(DashboardDto.KpiSummaryDto::getCategoryName));

        return categories.stream().map(cat -> {
            List<DashboardDto.KpiSummaryDto> catKpis =
                    byCategory.getOrDefault(cat.getName(), Collections.emptyList());
            double avg = catKpis.stream()
                    .mapToDouble(DashboardDto.KpiSummaryDto::getAchievementRate)
                    .average()
                    .orElse(0.0);
            return DashboardDto.CategorySummaryDto.builder()
                    .categoryId(cat.getId())
                    .categoryName(cat.getName())
                    .categoryColor(cat.getColor())
                    .categoryIcon(cat.getIcon())
                    .totalKpis(catKpis.size())
                    .avgAchievementRate(Math.round(avg * 10.0) / 10.0)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─── 최근 14일 추이 차트 데이터 ────────────────────────────────────────────────

    private List<DashboardDto.TrendPoint> buildTrendPoints(List<Kpi> activeKpis, LocalDate today) {
        if (activeKpis.isEmpty()) return Collections.emptyList();

        LocalDate start = today.minusDays(13);
        List<Long> kpiIds = activeKpis.stream().map(Kpi::getId).collect(Collectors.toList());
        List<KpiRecord> records = kpiRecordMapper.findByKpiIdsAndDateRange(kpiIds, start, today);

        Map<LocalDate, List<KpiRecord>> byDate = records.stream()
                .collect(Collectors.groupingBy(KpiRecord::getRecordedDate));
        Map<Long, Kpi> kpiMap = activeKpis.stream()
                .collect(Collectors.toMap(Kpi::getId, k -> k));

        List<DashboardDto.TrendPoint> points = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<KpiRecord> dayRecords = byDate.getOrDefault(date, Collections.emptyList());

            double avgRate = dayRecords.stream()
                    .mapToDouble(r -> {
                        Kpi kpi = kpiMap.get(r.getKpiId());
                        return kpi == null ? 0 : calculateAchievementRate(kpi, r.getActualValue(), r.getBooleanValue());
                    })
                    .average()
                    .orElse(0.0);

            points.add(DashboardDto.TrendPoint.builder()
                    .date(date.toString())
                    .avgAchievementRate(Math.round(avgRate * 10.0) / 10.0)
                    .recordedCount(dayRecords.size())
                    .build());
        }
        return points;
    }

    // ─── 주기별 현황 요약 (DAILY / WEEKLY / MONTHLY) ─────────────────────────────

    private DashboardDto.PeriodSummary buildPeriodSummary(List<Kpi> activeKpis, LocalDate today) {
        List<Kpi> daily   = activeKpis.stream().filter(k -> "DAILY".equals(k.getFrequency())).collect(Collectors.toList());
        List<Kpi> weekly  = activeKpis.stream().filter(k -> "WEEKLY".equals(k.getFrequency())).collect(Collectors.toList());
        List<Kpi> monthly = activeKpis.stream().filter(k -> "MONTHLY".equals(k.getFrequency())).collect(Collectors.toList());

        LocalDate weekStart  = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        return DashboardDto.PeriodSummary.builder()
                .dailyTotal(daily.size())
                .dailyRecorded(countRecordedInPeriodList(daily, today, today))
                .weeklyTotal(weekly.size())
                .weeklyRecorded(countRecordedInPeriodList(weekly, weekStart, today))
                .weekStart(weekStart.toString())
                .monthlyTotal(monthly.size())
                .monthlyRecorded(countRecordedInPeriodList(monthly, monthStart, today))
                .monthStart(monthStart.toString())
                .build();
    }

    private int countRecordedInPeriodList(List<Kpi> kpis, LocalDate start, LocalDate end) {
        return (int) kpis.stream()
                .filter(kpi -> kpiRecordMapper.countByPeriodAndKpiId(kpi.getId(), start, end) > 0)
                .count();
    }
}
