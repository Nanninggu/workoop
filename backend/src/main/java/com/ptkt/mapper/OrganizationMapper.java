package com.ptkt.mapper;

import com.ptkt.model.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrganizationMapper {
    void insert(Organization org);
    Optional<Organization> findById(Long id);
    Optional<Organization> findBySlug(String slug);
    Optional<Organization> findByInviteCode(String code);
    List<Organization> findByUserId(Long userId);
    void updateInviteCode(@Param("id") Long id,
                          @Param("inviteCode") String inviteCode,
                          @Param("inviteExpiresAt") java.time.LocalDateTime expiresAt);
}
