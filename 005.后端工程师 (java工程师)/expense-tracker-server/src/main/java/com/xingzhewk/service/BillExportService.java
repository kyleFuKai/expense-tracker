package com.xingzhewk.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 账单导出服务接口
 */
public interface BillExportService {

    /**
     * 导出账单数据
     *
     * @param userId     用户 ID
     * @param format     导出格式：csv 或 xlsx
     * @param month      月份筛选 yyyy-MM
     * @param type       账单类型 EXPENSE/INCOME
     * @param categoryId 分类筛选
     * @param keyword    备注关键词
     * @param startDate  起始日期 yyyy-MM-dd
     * @param endDate    结束日期 yyyy-MM-dd
     * @param response   HTTP 响应（用于写入文件流）
     */
    void exportBills(Long userId, String format, String month, String type,
                     Long categoryId, String keyword, String startDate, String endDate,
                     HttpServletResponse response);
}
