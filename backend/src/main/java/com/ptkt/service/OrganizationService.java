package com.ptkt.service;

import com.ptkt.mapper.MembershipMapper;
import com.ptkt.mapper.OrganizationMapper;
import com.ptkt.model.Membership;
import com.ptkt.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationMapper orgMapper;
    private final MembershipMapper membershipMapper;

    public Organization findById(Long id) {
        return orgMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다."));
    }

    public List<Organization> findByUserId(Long userId) {
        return orgMapper.findByUserId(userId);
    }

    public List<Membership> findMembers(Long orgId) {
        return membershipMapper.findByOrgId(orgId);
    }

    @Transactional
    public Organization create(String name, Long ownerId) {
        String slug = generateSlug(name);
        Organization org = Organization.builder()
                .name(name)
                .slug(slug)
                .build();
        orgMapper.insert(org);

        Membership ownership = Membership.builder()
                .orgId(org.getId())
                .userId(ownerId)
                .role("OWNER")
                .build();
        membershipMapper.insert(ownership);
        return org;
    }

    @Transactional
    public Organization generateInviteCode(Long orgId, Long requesterId) {
        Membership m = getMembership(orgId, requesterId);
        if ("MEMBER".equals(m.getRole())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        orgMapper.updateInviteCode(orgId, code, expiresAt);
        return findById(orgId);
    }

    @Transactional
    public Organization joinByCode(String code, Long userId) {
        Organization org = orgMapper.findByInviteCode(code)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 초대 코드입니다."));
        membershipMapper.findByOrgAndUser(org.getId(), userId).ifPresent(m -> {
            throw new IllegalArgumentException("이미 조직의 멤버입니다.");
        });
        membershipMapper.insert(Membership.builder()
                .orgId(org.getId()).userId(userId).role("MEMBER").build());
        return org;
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }

    @Transactional
    public void removeMember(Long orgId, Long userId) {
        membershipMapper.delete(orgId, userId);
    }

    @Transactional
    public void changeRole(Long orgId, Long userId, String role) {
        getMembership(orgId, userId);
        membershipMapper.updateRole(orgId, userId, role);
    }

    @Transactional
    public void addMember(Long orgId, Long userId, String role) {
        membershipMapper.findByOrgAndUser(orgId, userId).ifPresent(m -> {
            throw new IllegalArgumentException("이미 조직의 멤버입니다.");
        });
        membershipMapper.insert(Membership.builder()
                .orgId(orgId).userId(userId).role(role).build());
    }

    public Membership getMembership(Long orgId, Long userId) {
        return membershipMapper.findByOrgAndUser(orgId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 조직의 멤버가 아닙니다."));
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9가-힣]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "org";
        String slug = base;
        int suffix = 1;
        while (orgMapper.findBySlug(slug).isPresent()) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }
}
