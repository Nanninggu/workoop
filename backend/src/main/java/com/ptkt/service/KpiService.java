package com.ptkt.service;

import com.ptkt.mapper.KpiMapper;
import com.ptkt.model.Kpi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiService {

    private final KpiMapper kpiMapper;

    public List<Kpi> findAll(Long ownerId) {
        return kpiMapper.findByOwnerId(ownerId);
    }

    public List<Kpi> findActive(Long ownerId) {
        return kpiMapper.findByOwnerIdAndStatus(ownerId, "ACTIVE");
    }

    public List<Kpi> findByCategoryId(Long ownerId, Long categoryId) {
        return kpiMapper.findByOwnerIdAndCategoryId(ownerId, categoryId);
    }

    public List<Kpi> findTeamKpis(Long orgId) {
        return kpiMapper.findTeamKpisByOrgId(orgId);
    }

    public Kpi findById(Long id) {
        return kpiMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KPI를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Kpi create(Kpi kpi) {
        if (kpi.getStatus() == null) kpi.setStatus("ACTIVE");
        if (kpi.getSortOrder() == null) kpi.setSortOrder(0);
        if (kpi.getScope() == null) kpi.setScope("PERSONAL");
        kpiMapper.insert(kpi);
        return findById(kpi.getId());
    }

    @Transactional
    public Kpi update(Long id, Kpi kpi) {
        findById(id);
        kpi.setId(id);
        kpiMapper.update(kpi);
        return findById(id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        findById(id);
        kpiMapper.updateStatus(id, status);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        kpiMapper.deleteById(id);
    }
}
