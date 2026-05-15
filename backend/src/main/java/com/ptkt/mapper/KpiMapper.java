package com.ptkt.mapper;

import com.ptkt.model.Kpi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface KpiMapper {
    // 개인 KPI (owner_id 기반)
    List<Kpi> findByOwnerId(@Param("ownerId") Long ownerId);
    List<Kpi> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") String status);
    List<Kpi> findByOwnerIdAndCategoryId(@Param("ownerId") Long ownerId, @Param("categoryId") Long categoryId);

    // 팀 KPI (org_id 기반)
    List<Kpi> findTeamKpisByOrgId(@Param("orgId") Long orgId);

    Optional<Kpi> findById(Long id);
    int insert(Kpi kpi);
    int update(Kpi kpi);
    int deleteById(Long id);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    int deleteAll();
    int insertWithId(Kpi kpi);
}
