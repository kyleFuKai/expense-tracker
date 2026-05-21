package com.xingzhewk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xingzhewk.common.Result;
import com.xingzhewk.dto.BillDTO;
import com.xingzhewk.vo.BillStatsVO;

/**
 * 账单服务接口
 *
 * 处理账单的 CRUD、分页查询、月度统计等业务逻辑。
 */
public interface BillService {

    /**
     * 分页查询账单列表
     *
     * @param userId     用户 ID
     * @param month      月份筛选（yyyy-MM），为空则查询全部
     * @param categoryId 分类 ID 筛选
     * @param type       账单类型筛选（EXPENSE/INCOME）
     * @param keyword    备注关键词搜索（模糊匹配）
     * @param page       页码（从 1 开始）
     * @param pageSize   每页条数（最大 100）
     * @return {list: [...], total: N, page: 1, pageSize: 50}
     */
    Result<?> list(Long userId, String month, Long categoryId, String type, String keyword, int page, int pageSize);

    /**
     * 获取账单详情
     *
     * @param userId 用户 ID
     * @param id     账单 ID
     * @return 账单对象，code=404 表示不存在
     */
    Result<Object> getById(Long userId, Long id);

    /**
     * 创建账单
     *
     * @param userId 用户 ID
     * @param dto    账单参数（type、amount、categoryId、remark、billTime）
     * @return {id: 新账单ID}
     */
    Result<?> create(Long userId, BillDTO dto);

    /**
     * 修改账单
     *
     * @param userId 用户 ID
     * @param id     账单 ID
     * @param dto    账单参数，同创建接口
     * @return 空，code=404 表示不存在
     */
    Result<Void> update(Long userId, Long id, BillDTO dto);

    /**
     * 删除账单
     *
     * @param userId 用户 ID
     * @param id     账单 ID
     * @return 空，code=404 表示不存在
     */
    Result<Void> delete(Long userId, Long id);

    /**
     * 月度收支统计（总支出、总收入、日趋势、分类排行）
     *
     * @param userId 用户 ID
     * @param month  月份（yyyy-MM），为空则使用当前月
     * @return {expense, income, daily: [...], categories: [...]}
     */
    Result<BillStatsVO> monthlyStats(Long userId, String month);
}
