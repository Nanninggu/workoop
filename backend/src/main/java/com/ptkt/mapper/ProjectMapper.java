package com.ptkt.mapper;

import com.ptkt.model.Project;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProjectMapper {
    void insert(Project project);
    Optional<Project> findById(Long id);
    List<Project> findByOrgId(Long orgId);
    void update(Project project);
    void deleteById(Long id);
}
