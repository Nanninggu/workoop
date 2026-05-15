package com.ptkt.service;

import com.ptkt.mapper.CategoryMapper;
import com.ptkt.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> findAll() {
        return categoryMapper.findAll();
    }

    public List<Category> findByUser(Long ownerId) {
        return categoryMapper.findByOwnerIdOrGlobal(ownerId);
    }

    public Category findById(Long id) {
        return categoryMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Category create(Category category) {
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public Category update(Long id, Category category) {
        findById(id);
        category.setId(id);
        categoryMapper.update(category);
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        int kpiCount = categoryMapper.countKpisByCategoryId(id);
        if (kpiCount > 0) {
            throw new IllegalStateException("해당 카테고리에 KPI가 존재하여 삭제할 수 없습니다.");
        }
        categoryMapper.deleteById(id);
    }
}
