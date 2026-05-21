package com.xingzhewk.service;

import com.xingzhewk.common.Result;
import com.xingzhewk.dto.BudgetDTO;
import com.xingzhewk.vo.BudgetDashboardVO;

import java.util.List;

/**
 * 预算服务接口
 *
 * 处理预算的创建、查询、仪表盘统计等业务逻辑。
 */
public interface BudgetService {

    /**
     * 获取预算列表
     *
     * @param userId 用户 ID
     * @param type   预算类型筛选，为空则查询全部
     * @return 预算列表
     */
    Result<List<?>> list(Long userId, String type);

    /**
     * 预算仪表盘（当月总预算、已用、剩余、各分类进度）
     *
     * @param userId 用户 ID
     * @param month  月份（yyyy-MM），为空则使用当前月
     * @return {totalBudget, spent, remaining, percent, categories: [...]}
     */
    Result<BudgetDashboardVO> dashboard(Long userId, String month);

    /**
     * 创建或更新预算（同月同分类的预算自动覆盖）
     *
     * @param userId 用户 ID
     * @param dto    预算参数（categoryId、amount、period、startDate、endDate）
     * @return {id: 预算ID}
     */
    Result<Long> createOrUpdate(Long userId, BudgetDTO dto);

    /**
     * 停用预算（软删除）
     *
     * @param userId 用户 ID
     * @param id     预算 ID
     * @return 空，code=404 表示不存在
     */
    Result<Void> delete(Long userId, Long id);
}
