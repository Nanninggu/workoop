package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.Membership;
import com.ptkt.model.Organization;
import com.ptkt.model.User;
import com.ptkt.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Organization>>> myOrgs(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(orgService.findByUserId(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Organization>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        Organization org = orgService.create(body.get("name"), user.getId());
        return ResponseEntity.ok(ApiResponse.ok("조직이 생성되었습니다.", org));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<ApiResponse<List<Membership>>> members(
            @AuthenticationPrincipal User user,
            @PathVariable Long orgId) {
        orgService.getMembership(orgId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(orgService.findMembers(orgId)));
    }

    @PostMapping("/{orgId}/invite")
    public ResponseEntity<ApiResponse<Organization>> generateInvite(
            @AuthenticationPrincipal User user,
            @PathVariable Long orgId) {
        Organization org = orgService.generateInviteCode(orgId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("초대 코드가 생성되었습니다.", org));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Organization>> joinByCode(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        Organization org = orgService.joinByCode(body.get("code"), user.getId());
        return ResponseEntity.ok(ApiResponse.ok("조직에 참여했습니다.", org));
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long orgId,
            @PathVariable Long userId) {
        Membership my = orgService.getMembership(orgId, user.getId());
        if ("MEMBER".equals(my.getRole()) && !user.getId().equals(userId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("권한이 없습니다."));
        }
        orgService.removeMember(orgId, userId);
        return ResponseEntity.ok(ApiResponse.ok("멤버가 제거되었습니다.", null));
    }

    @PatchMapping("/{orgId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            @AuthenticationPrincipal User user,
            @PathVariable Long orgId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        Membership my = orgService.getMembership(orgId, user.getId());
        if ("MEMBER".equals(my.getRole())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("권한이 없습니다."));
        }
        orgService.changeRole(orgId, userId, body.get("role"));
        return ResponseEntity.ok(ApiResponse.ok("역할이 변경되었습니다.", null));
    }

    @PostMapping("/{orgId}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long orgId,
            @RequestBody Map<String, String> body) {
        Membership myMembership = orgService.getMembership(orgId, user.getId());
        if ("MEMBER".equals(myMembership.getRole())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("권한이 없습니다."));
        }
        Long targetUserId = Long.valueOf(body.get("userId"));
        String role = body.getOrDefault("role", "MEMBER");
        orgService.addMember(orgId, targetUserId, role);
        return ResponseEntity.ok(ApiResponse.ok("멤버가 추가되었습니다.", null));
    }
}
