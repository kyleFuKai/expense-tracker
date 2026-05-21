package com.expense.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.expense.common.Result;
import com.expense.dto.CategoryDTO;
import com.expense.entity.Category;
import com.expense.common.exception.BusinessException;
import com.expense.mapper.CategoryMapper;
import com.expense.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public Result<List<?>> list(Long userId, String type) {
        if (StringUtils.hasText(type) && !"EXPENSE".equalsIgnoreCase(type) && !"INCOME".equalsIgnoreCase(type)) {
            throw new BusinessException(400, "分类类型不合法");
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Category::getUserId, userId).or().eq(Category::getUserId, 0));
        if (StringUtils.hasText(type)) {
            wrapper.eq(Category::getType, type.toUpperCase());
        }
        wrapper.eq(Category::getIsArchived, 0);
        wrapper.orderByAsc(Category::getSortOrder, Category::getId);

        List<Category> categories = categoryMapper.selectList(wrapper);
        return Result.success(categories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> create(Long userId, CategoryDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank() || dto.getType() == null) {
            throw new BusinessException(400, "分类名称和类型不能为空");
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon() != null ? dto.getIcon() : "");
        category.setType(dto.getType().toUpperCase());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setIsPreset(0);
        category.setUserId(userId);

        categoryMapper.insert(category);
        log.info("创建分类, categoryId={}, name={}", category.getId(), category.getName());
        Map<String, Object> createResult = new HashMap<>();
        createResult.put("id", category.getId());
        return Result.success(createResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long userId, Long id, CategoryDTO dto) {
        Category existing = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, id).eq(Category::getUserId, userId));
        if (existing == null) {
            throw new BusinessException(404, "分类不存在");
        }

        Category update = new Category();
        update.setId(id);
        if (dto.getName() != null) update.setName(dto.getName());
        if (dto.getIcon() != null) update.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());

        categoryMapper.updateById(update);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long userId, Long id) {
        Category existing = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, id).eq(Category::getUserId, userId).eq(Category::getIsPreset, 0));
        if (existing == null) {
            throw new BusinessException(404, "分类不存在或为系统预设分类");
        }

        long billCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getId, id));

        if (billCount > 0) {
            existing.setIsArchived(1);
            categoryMapper.updateById(existing);
            log.info("分类已归档, categoryId={}", id);
        } else {
            categoryMapper.deleteById(id);
        }
        return Result.success();
    }
}
