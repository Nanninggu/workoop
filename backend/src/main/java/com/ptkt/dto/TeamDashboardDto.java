package com.ptkt.dto;

import lombok.*;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class TeamDashboardDto {

    private List<TeamKpiDto>          teamKpis;
    private List<MemberContributionDto> memberContributions;
    private List<VelocityPoint>       velocity;
    private List<BlockerDto>          blockers;
    private List<EnergyRowDto>        energyMap;
    private MemberContributionDto     mvp;
    private List<BurnoutMemberDto>    burnoutRisks;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class TeamKpiDto {
        private Long   kpiId;
        private String name;
        private String unit;
        private String kpiType;
        private double targetValue;
        private double currentValue;
        private double achievementRate;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class MemberContributionDto {
        private Long   userId;
        private String userName;
        private long   doneTasks;
        private double kpiContribution;
        private long   totalTasks;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class VelocityPoint {
        private String date;
        private long   doneCount;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class BlockerDto {
        private Long   userId;
        private String userName;
        private String blocker;
        private String severity;
        private String date;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class EnergyRowDto {
        private Long         userId;
        private String       userName;
        private List<String> dates;
        private List<Integer> values;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class BurnoutMemberDto {
        private Long         userId;
        private String       userName;
        private String       riskLevel;
        private List<String> signals;
    }
}
