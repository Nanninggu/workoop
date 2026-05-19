package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.mapper.ReviewMapper;
import com.ptkt.model.Review;
import com.ptkt.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewMapper reviewMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Review>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(reviewMapper.findByUser(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Review review = Review.builder()
                .userId(user.getId())
                .type((String) body.get("type"))
                .period((String) body.get("period"))
                .plans((String) body.get("plans"))
                .progress((String) body.get("progress"))
                .problems((String) body.get("problems"))
                .bestAchievement((String) body.get("bestAchievement"))
                .regrets((String) body.get("regrets"))
                .nextGoals((String) body.get("nextGoals"))
                .memo((String) body.get("memo"))
                .selfScore(body.get("selfScore") != null
                        ? Integer.valueOf(body.get("selfScore").toString()) : 3)
                .build();
        reviewMapper.insert(review);
        return ResponseEntity.ok(ApiResponse.ok("저장됐습니다.", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        reviewMapper.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("삭제됐습니다.", null));
    }
}
