package com.xingzhewk.service;

import com.xingzhewk.common.Result;
import com.xingzhewk.dto.CategoryDTO;

import java.util.List;

/**
 * 分类服务接口
 *
 * 处理分类的查询、创建、修改、软归档等业务逻辑。
 */
public interface CategoryService {

    /**
     * 获取分类列表
     *
     * @param userId 用户 ID
     * @param type   分类类型筛选（expense/income），为空则查询全部
     * @return 分类列表（含图标、名称、排序等）
     */
    Result<List<?>> list(Long userId, String type);

    /**
     * 创建分类
     *
     * @param userId 用户 ID
     * @param dto    分类参数（name、icon、type、parentId、sortOrder）
     * @return {id: 新分类ID}
     */
    Result<?> create(Long userId, CategoryDTO dto);

    /**
     * 修改分类
     *
     * @param userId 用户 ID
     * @param id     分类 ID
     * @param dto    分类参数，同创建接口
     * @return 空，code=404 表示不存在
     */
    Result<Void> update(Long userId, Long id, CategoryDTO dto);

    /**
     * 删除分类（软归档）
     *
     * @param userId 用户 ID
     * @param id     分类 ID
     * @return 空，code=404 表示不存在
     */
    Result<Void> delete(Long userId, Long id);
}
