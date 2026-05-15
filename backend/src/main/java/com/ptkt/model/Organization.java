package com.ptkt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {
    private Long id;
    private String name;
    private String slug;
    private String inviteCode;
    private LocalDateTime inviteExpiresAt;
    private LocalDateTime createdAt;

    // JOIN field
    private List<Membership> members;
}
