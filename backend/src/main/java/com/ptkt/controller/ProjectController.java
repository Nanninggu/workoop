package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.dto.WsEvent;
import com.ptkt.model.Project;
import com.ptkt.model.User;
import com.ptkt.service.ProjectService;
import com.ptkt.service.RealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final RealtimeService realtimeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Project>>> list(@RequestParam Long orgId) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.findByOrgId(orgId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Project>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        Long orgId = Long.valueOf(body.get("orgId"));
        Project p = projectService.create(body.get("name"), body.get("description"), orgId, user.getId());
        realtimeService.broadcastToOrg(orgId, "projects", WsEvent.builder()
                .type("PROJECT_CREATED").payload(p).orgId(orgId).actorId(user.getId()).build());
        return ResponseEntity.ok(ApiResponse.ok("프로젝트가 생성되었습니다.", p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Project p = projectService.update(id, body.get("name"), body.get("description"));
        realtimeService.broadcastToOrg(p.getOrgId(), "projects", WsEvent.builder()
                .type("PROJECT_UPDATED").payload(p).orgId(p.getOrgId()).actorId(user.getId()).build());
        return ResponseEntity.ok(ApiResponse.ok("수정되었습니다.", p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Project p = projectService.findById(id);
        projectService.delete(id);
        realtimeService.broadcastToOrg(p.getOrgId(), "projects", WsEvent.builder()
                .type("PROJECT_DELETED").payload(Map.of("id", id)).orgId(p.getOrgId()).actorId(user.getId()).build());
        return ResponseEntity.ok(ApiResponse.ok("삭제되었습니다.", null));
    }
}
