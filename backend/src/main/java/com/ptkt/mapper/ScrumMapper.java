package com.ptkt.mapper;

import com.ptkt.model.Scrum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface ScrumMapper {
    void upsert(Scrum scrum);
    Optional<Scrum> findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    List<Scrum> findTeamByDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);
    List<Scrum> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    List<Scrum> findActiveBlockers(@Param("orgId") Long orgId, @Param("date") LocalDate date);
    List<Map<String, Object>> findTeamEnergyMap(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
