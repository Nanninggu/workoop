package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.mapper.StarNoteMapper;
import com.ptkt.model.StarNote;
import com.ptkt.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/star-notes")
@RequiredArgsConstructor
public class StarNoteController {

    private final StarNoteMapper starNoteMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StarNote>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(starNoteMapper.findByUser(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StarNote>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        StarNote note = buildNote(body, user.getId());
        starNoteMapper.insert(note);
        return ResponseEntity.ok(ApiResponse.ok("저장됐습니다.", starNoteMapper.findById(note.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        StarNote note = buildNote(body, user.getId());
        note.setId(id);
        starNoteMapper.update(note);
        return ResponseEntity.ok(ApiResponse.ok("수정됐습니다.", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        starNoteMapper.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("삭제됐습니다.", null));
    }

    private StarNote buildNote(Map<String, Object> body, Long userId) {
        return StarNote.builder()
                .userId(userId)
                .title((String) body.get("title"))
                .lpTag((String) body.get("lpTag"))
                .kpiId(body.get("kpiId") != null ? Long.valueOf(body.get("kpiId").toString()) : null)
                .situation((String) body.get("situation"))
                .task((String) body.get("task"))
                .action((String) body.get("action"))
                .result((String) body.get("result"))
                .build();
    }
}
