package com.ptkt.mapper;

import com.ptkt.model.Membership;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MembershipMapper {
    void insert(Membership membership);
    Optional<Membership> findByOrgAndUser(Long orgId, Long userId);
    List<Membership> findByOrgId(Long orgId);
    List<Membership> findByUserId(Long userId);
    void updateRole(Long orgId, Long userId, String role);
    void delete(Long orgId, Long userId);
}
