package com.ptkt.service;

import com.ptkt.dto.KpiRecordRequest;
import com.ptkt.mapper.KpiRecordMapper;
import com.ptkt.model.Kpi;
import com.ptkt.model.KpiRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiRecordService {

    private final KpiRecordMapper kpiRecordMapper;
    private final KpiService kpiService;

    public List<KpiRecord> findByKpiId(Long kpiId) {
        return kpiRecordMapper.findByKpiId(kpiId);
    }

    public List<KpiRecord> findByKpiIdAndDateRange(Long kpiId, LocalDate start, LocalDate end) {
        return kpiRecordMapper.findByKpiIdAndDateRange(kpiId, start, end);
    }

    public List<KpiRecord> findByDateRange(LocalDate start, LocalDate end) {
        return kpiRecordMapper.findByDateRange(start, end);
    }

    public List<KpiRecord> findByDate(LocalDate date) {
        return kpiRecordMapper.findByDate(date);
    }

    public Optional<KpiRecord> findByKpiIdAndDate(Long kpiId, LocalDate date) {
        return kpiRecordMapper.findByKpiIdAndDate(kpiId, date);
    }

    @Transactional
    public KpiRecord upsert(KpiRecordRequest req) {
        Kpi kpi = kpiService.findById(req.getKpiId());

        LocalDate recordDate = req.getRecordedDate() != null ? req.getRecordedDate() : LocalDate.now();

        Optional<KpiRecord> existing = kpiRecordMapper.findByKpiIdAndDate(req.getKpiId(), recordDate);

        KpiRecord record;
        if (existing.isPresent()) {
            record = existing.get();
            record.setActualValue(req.getActualValue());
            record.setBooleanValue(req.getBooleanValue());
            record.setNote(req.getNote());
            record.setRecordedDate(recordDate);
            kpiRecordMapper.update(record);
            return kpiRecordMapper.findById(record.getId()).orElse(record);
        } else {
            record = KpiRecord.builder()
                    .kpiId(req.getKpiId())
                    .actualValue(req.getActualValue())
                    .booleanValue(req.getBooleanValue())
                    .recordedDate(recordDate)
                    .note(req.getNote())
                    .build();
            kpiRecordMapper.insert(record);
            return record;
        }
    }

    @Transactional
    public void delete(Long id) {
        kpiRecordMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기록을 찾을 수 없습니다. id=" + id));
        kpiRecordMapper.deleteById(id);
    }
}
