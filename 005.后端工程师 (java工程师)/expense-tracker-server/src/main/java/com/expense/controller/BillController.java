package com.expense.controller;

import com.expense.common.Result;
import com.expense.common.Constants;
import com.expense.dto.BillDTO;
import com.expense.service.BillService;
import com.expense.vo.BillStatsVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账单控制器
 *
 * 处理账单的 CRUD、月度统计等接口。所有接口均需 JWT 认证。
 */
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    /**
     * 获取账单列表（分页）
     *
     * @param month       月份筛选（yyyy-MM），为空则查询全部
     * @param category_id 分类 ID 筛选
     * @param type        账单类型，可选：EXPENSE（支出）、INCOME（收入），为空则查询全部
     * @param keyword     备注关键词搜索（模糊匹配）
     * @param page        页码，从 1 开始，默认 1
     * @param pageSize    每页条数，默认 50，最大 100
     * @return {list: [...], total: N, page: 1, pageSize: 50}
     */
    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(required = false) String month,
                          @RequestParam(required = false) Long category_id,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "50") int pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        pageSize = Math.max(1, Math.min(Constants.MAX_PAGE_SIZE, pageSize));
        return billService.list(userId, month, category_id, type, keyword, page, pageSize);
    }

    /**
     * 获取账单详情
     *
     * @param id 账单 ID
     * @return 账单对象，code=404 表示账单不存在
     */
    @GetMapping("/{id}")
    public Result<?> getById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return billService.getById(userId, id);
    }

    /**
     * 创建账单
     *
     * @param dto 账单参数，包含 type（EXPENSE/INCOME）、amount（金额，必须 > 0）、categoryId、remark（可选，最长 200 字符）、billTime（可选，默认当前时间）
     * @return {id: 新账单ID}
     */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @Valid @RequestBody BillDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return billService.create(userId, dto);
    }

    /**
     * 修改账单
     *
     * @param id  账单 ID
     * @param dto 账单参数，同创建接口
     * @return 空，code=404 表示账单不存在
     */
    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody BillDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return billService.update(userId, id, dto);
    }

    /**
     * 删除账单
     *
     * @param id 账单 ID
     * @return 空，code=404 表示账单不存在
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return billService.delete(userId, id);
    }

    /**
     * 月度收支统计
     *
     * @param month 月份（yyyy-MM），为空则使用当前月
     * @return {expense: 总支出, income: 总收入, daily: 日趋势[], categories: 分类排行[]}
     */
    @GetMapping("/stats/month")
    public Result<BillStatsVO> monthlyStats(HttpServletRequest request,
                                            @RequestParam(required = false) String month) {
        Long userId = (Long) request.getAttribute("userId");
        return billService.monthlyStats(userId, month);
    }
}
