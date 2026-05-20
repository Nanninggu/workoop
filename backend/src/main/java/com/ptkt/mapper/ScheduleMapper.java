package com.ptkt.mapper;

import com.ptkt.model.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ScheduleMapper {
    void insert(Schedule schedule);
    List<Schedule> findByUserAndDateRange(@Param("userId") Long userId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);
    void deleteById(@Param("id") Long id);
}
