package com.ptkt.mapper;

import com.ptkt.model.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CategoryMapper {
    List<Category> findAll();
    List<Category> findByOwnerIdOrGlobal(Long ownerId);
    Optional<Category> findById(Long id);
    int insert(Category category);
    int update(Category category);
    int deleteById(Long id);
    int countKpisByCategoryId(Long categoryId);
    int deleteAll();
    int insertWithId(Category category);
}
