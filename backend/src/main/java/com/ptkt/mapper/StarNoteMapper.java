package com.ptkt.mapper;

import com.ptkt.model.StarNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StarNoteMapper {
    List<StarNote> findByUser(@Param("userId") Long userId);
    StarNote findById(@Param("id") Long id);
    void insert(StarNote note);
    void update(StarNote note);
    void delete(@Param("id") Long id, @Param("userId") Long userId);
}
