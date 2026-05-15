package com.ptkt.service;

import com.ptkt.mapper.ProjectMapper;
import com.ptkt.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectMapper projectMapper;

    public List<Project> findByOrgId(Long orgId) {
        return projectMapper.findByOrgId(orgId);
    }

    public Project findById(Long id) {
        return projectMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));
    }

    @Transactional
    public Project create(String name, String description, Long orgId, Long ownerId) {
        Project p = Project.builder()
                .orgId(orgId).name(name).description(description).ownerId(ownerId)
                .build();
        projectMapper.insert(p);
        return findById(p.getId());
    }

    @Transactional
    public Project update(Long id, String name, String description) {
        Project p = findById(id);
        p.setName(name);
        p.setDescription(description);
        projectMapper.update(p);
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        projectMapper.deleteById(id);
    }
}
