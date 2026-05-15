package com.ptkt.controller;

import com.ptkt.dto.ApiResponse;
import com.ptkt.model.Category;
import com.ptkt.model.User;
import com.ptkt.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.findByUser(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Category category) {
        category.setOwnerId(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("카테고리가 생성되었습니다.", categoryService.create(category)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(@PathVariable Long id, @RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.ok("카테고리가 수정되었습니다.", categoryService.update(id, category)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("카테고리가 삭제되었습니다.", null));
    }
}
