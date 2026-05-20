package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.mapper.ScheduleMapper;
import com.ptkt.model.Schedule;
import com.ptkt.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleMapper scheduleMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Schedule>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleMapper.findByUserAndDateRange(user.getId(), from, to)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Schedule>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Schedule s = new Schedule();
        s.setUserId(user.getId());
        s.setTitle((String) body.get("title"));
        s.setEventDate(LocalDate.parse((String) body.get("eventDate")));
        s.setEventTime((String) body.getOrDefault("eventTime", null));
        s.setSource("SLACK");
        scheduleMapper.insert(s);
        return ResponseEntity.ok(ApiResponse.ok(s));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        scheduleMapper.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
