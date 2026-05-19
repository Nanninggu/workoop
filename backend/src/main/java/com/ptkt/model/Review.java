package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    private Long id;
    private Long userId;
    private String type;            // weekly, monthly
    private String period;
    private String plans;
    private String progress;
    private String problems;
    private String bestAchievement;
    private String regrets;
    private String nextGoals;       // JSON 배열 문자열
    private String memo;
    private Integer selfScore;
    private LocalDateTime savedAt;
}
