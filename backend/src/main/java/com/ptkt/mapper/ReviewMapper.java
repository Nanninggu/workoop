package com.ptkt.mapper;

import com.ptkt.model.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    List<Review> findByUser(@Param("userId") Long userId);
    void insert(Review review);
    void delete(@Param("id") Long id, @Param("userId") Long userId);
}
