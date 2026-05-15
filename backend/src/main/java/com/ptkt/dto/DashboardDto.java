package com.ptkt.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDto {
    private int totalKpis;
    private int activeKpis;
    private int todayRecorded;          // 하위 호환 유지 (DAILY KPI 기준)
    private int recordedInPeriod;       // 각 주기에 맞는 기간 내 입력 완료 수
    private double overallAchievementRate;
    private List<KpiSummaryDto> kpiSummaries;
    private List<CategorySummaryDto> categorySummaries;
    private List<TrendPoint> trendPoints;   // 최근 14일 추이
    private PeriodSummary periodSummary;    // 주기별 현황 요약

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KpiSummaryDto {
        private Long kpiId;
        private String kpiName;
        private String categoryName;
        private String categoryColor;
        private String kpiType;
        private String unit;
        private BigDecimal targetValue;
        private BigDecimal latestValue;
        private Boolean latestBooleanValue;
        private double achievementRate;
        private String frequency;
        private String status;
        private boolean recordedToday;      // 하위 호환
        private boolean recordedInPeriod;   // 각 주기 기간 내 입력 여부
        private String periodLabel;         // "오늘" / "이번 주" / "이번 달"
        private int streak;                 // 연속 달성 일수 (DAILY KPI만)
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategorySummaryDto {
        private Long categoryId;
        private String categoryName;
        private String categoryColor;
        private String categoryIcon;
        private int totalKpis;
        private double avgAchievementRate;
    }

    /** 최근 14일 일별 추이 포인트 */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendPoint {
        private String date;
        private double avgAchievementRate;
        private int recordedCount;
    }

    /** DAILY / WEEKLY / MONTHLY 주기별 현황 요약 */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PeriodSummary {
        private int dailyTotal;
        private int dailyRecorded;
        private int weeklyTotal;
        private int weeklyRecorded;
        private String weekStart;
        private int monthlyTotal;
        private int monthlyRecorded;
        private String monthStart;
    }
}
