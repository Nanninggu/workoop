package com.ptkt.mapper;

import com.ptkt.model.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface TaskMapper {
    void insert(Task task);
    Optional<Task> findById(Long id);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssigneeId(@Param("assigneeId") Long assigneeId);
    List<Map<String, Object>> getMemberWorkloadByOrgId(@Param("orgId") Long orgId);
    List<Map<String, Object>> getMemberContributions(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    List<Map<String, Object>> getDailyVelocity(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    void update(Task task);
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    void deleteById(Long id);
}
