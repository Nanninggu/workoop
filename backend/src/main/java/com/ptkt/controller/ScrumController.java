package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.dto.WsEvent;
import com.ptkt.mapper.ScrumMapper;
import com.ptkt.model.Scrum;
import com.ptkt.model.User;
import com.ptkt.service.RealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scrums")
@RequiredArgsConstructor
public class ScrumController {

    private final ScrumMapper scrumMapper;
    private final RealtimeService realtimeService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> upsert(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        String dateStr = (String) body.getOrDefault("date", LocalDate.now().toString());
        Long orgId = body.get("orgId") != null
                ? Long.valueOf(body.get("orgId").toString()) : null;

        Scrum scrum = Scrum.builder()
                .userId(user.getId())
                .orgId(orgId)
                .scrumDate(LocalDate.parse(dateStr))
                .tasksJson((String) body.get("tasksJson"))
                .blocker((String) body.get("blocker"))
                .blockerSeverity((String) body.getOrDefault("blockerSeverity", "LOW"))
                .energy(body.get("energy") != null
                        ? Integer.valueOf(body.get("energy").toString()) : 0)
                .focus((String) body.get("focus"))
                .build();

        scrumMapper.upsert(scrum);

        // 같은 조직 팀원들에게 실시간 업데이트 전파
        realtimeService.broadcastToOrg(orgId, "scrums", WsEvent.builder()
                .type("SCRUM_UPDATED")
                .payload(scrum)
                .orgId(orgId)
                .actorId(user.getId())
                .build());

        return ResponseEntity.ok(ApiResponse.ok("스크럼이 저장되었습니다.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Scrum>> myScrum(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String date) {
        LocalDate d = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(
                scrumMapper.findByUserAndDate(user.getId(), d).orElse(null)
        ));
    }

    @GetMapping("/me/range")
    public ResponseEntity<ApiResponse<List<Scrum>>> myRange(
            @AuthenticationPrincipal User user,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(ApiResponse.ok(
                scrumMapper.findByUserAndDateRange(
                        user.getId(),
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate))
        ));
    }

    @GetMapping("/team")
    public ResponseEntity<ApiResponse<List<Scrum>>> teamScrums(
            @RequestParam Long orgId,
            @RequestParam(required = false) String date) {
        LocalDate d = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(
                scrumMapper.findTeamByDate(orgId, d)
        ));
    }
}
