package com.expense.controller;

import com.expense.common.Result;
import com.expense.dto.CategoryDTO;
import com.expense.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 分类控制器
 *
 * 处理分类的 CRUD 接口。所有接口均需 JWT 认证。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表
     *
     * @param type 分类类型筛选（expense 支出 / income 收入），为空则查询全部
     * @return 分类列表（含图标、名称、排序等）
     */
    @GetMapping
    public Result<?> list(HttpServletRequest request, @RequestParam(required = false) String type) {
        Long userId = (Long) request.getAttribute("userId");
        return categoryService.list(userId, type);
    }

    /**
     * 创建分类
     *
     * @param dto 分类参数，包含 name（名称）、icon（图标）、type（expense/income）、parentId（父分类，可选）、sortOrder（排序，可选）
     * @return {id: 新分类ID}
     */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @Valid @RequestBody CategoryDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return categoryService.create(userId, dto);
    }

    /**
     * 修改分类
     *
     * @param id  分类 ID
     * @param dto 分类参数，同创建接口
     * @return 空，code=404 表示分类不存在
     */
    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return categoryService.update(userId, id, dto);
    }

    /**
     * 删除分类（软归档）
     *
     * @param id 分类 ID
     * @return 空，code=404 表示分类不存在
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return categoryService.delete(userId, id);
    }
}
