package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.Task;
import com.ptkt.model.User;
import com.ptkt.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> list(@RequestParam Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.findByProjectId(projectId)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<Task>>> mine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.findMyTasks(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Task task) {
        return ResponseEntity.ok(ApiResponse.ok("태스크가 생성되었습니다.", taskService.create(task)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> update(
            @PathVariable Long id,
            @RequestBody Task task) {
        return ResponseEntity.ok(ApiResponse.ok("수정되었습니다.", taskService.update(id, task)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Task>> changeStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Task updated = taskService.changeStatus(id, body.get("status"), user.getId());
        return ResponseEntity.ok(ApiResponse.ok("상태가 변경되었습니다.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("삭제되었습니다.", null));
    }
}
