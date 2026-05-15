package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.dto.DashboardDto;
import com.ptkt.model.User;
import com.ptkt.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardDto>> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(user.getId())));
    }
}
