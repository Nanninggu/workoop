package com.ptkt.controller;

import com.ptkt.config.JwtUtil;
import com.ptkt.dto.ApiResponse;
import com.ptkt.dto.AuthRequest;
import com.ptkt.model.User;
import com.ptkt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(@RequestBody AuthRequest req) {
        User user = userService.register(req.getEmail(), req.getPassword(), req.getName());
        String token = jwtUtil.generate(user.getId(), user.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다.", buildAuthPayload(user, token)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody AuthRequest req) {
        User user = userService.findByEmail(req.getEmail());
        if (!userService.checkPassword(user, req.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 또는 비밀번호가 올바르지 않습니다."));
        }
        String token = jwtUtil.generate(user.getId(), user.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("로그인 성공", buildAuthPayload(user, token)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName()
        )));
    }

    private Map<String, Object> buildAuthPayload(User user, String token) {
        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName()
                )
        );
    }
}
