package com.ptkt.mapper;

import com.ptkt.model.KpiRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface KpiRecordMapper {
    List<KpiRecord> findByKpiId(@Param("kpiId") Long kpiId);
    List<KpiRecord> findByKpiIdAndDateRange(
            @Param("kpiId") Long kpiId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    List<KpiRecord> findByDate(@Param("date") LocalDate date);
    Optional<KpiRecord> findByKpiIdAndDate(@Param("kpiId") Long kpiId, @Param("date") LocalDate date);
    Optional<KpiRecord> findById(Long id);
    int insert(KpiRecord record);
    int update(KpiRecord record);
    int deleteById(Long id);
    int countByDateAndKpiIds(@Param("date") LocalDate date, @Param("kpiIds") List<Long> kpiIds);

    /** 특정 기간 내 가장 최근 기록 조회 (주간/월간 KPI 달성 기준에 사용) */
    Optional<KpiRecord> findLatestByKpiIdBetween(
            @Param("kpiId") Long kpiId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 특정 기간 내 기록 건수 (기간 내 입력 여부 확인에 사용) */
    int countByPeriodAndKpiId(
            @Param("kpiId") Long kpiId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 해당 KPI 의 모든 기록 날짜 목록 (연속달성 Streak 계산에 사용) */
    List<LocalDate> findRecordedDatesByKpiId(@Param("kpiId") Long kpiId);

    /** 여러 KPI 의 특정 기간 기록 일괄 조회 (추이 차트에 사용) */
    List<KpiRecord> findByKpiIdsAndDateRange(
            @Param("kpiIds") List<Long> kpiIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 전체 KPI 날짜 범위 기록 조회 (캘린더/분석 뷰용) */
    List<KpiRecord> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 전체 기록 (백업 내보내기용) */
    List<KpiRecord> findAll();

    /** 전체 삭제 (백업 복원 전처리용) */
    int deleteAll();

    /** ID 지정 삽입 (백업 복원용) */
    int insertWithId(KpiRecord record);
}
