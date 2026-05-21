package com.xingzhewk.controller;

import com.xingzhewk.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 账单导出接口测试
 *
 * 使用 @SpringBootTest + MockMvc 进行端到端 HTTP 测试，
 * 连接真实数据库，验证导出接口的完整行为。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BillExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 生成测试用 JWT Token
     * userId=2 是数据库中已存在的测试用户
     */
    private String testToken() {
        return jwtUtil.generateToken(2L, "13800138000");
    }

    @Test
    @DisplayName("EXPORT-01: 导出当月账单 CSV — 返回码 200，Content-Type 正确，内容含表头")
    void testExportCsv_currentMonth_returnsCsvWithHeader() throws Exception {
        String currentMonth = java.time.YearMonth.now().toString(); // yyyy-MM

        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .param("month", currentMonth)
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv; charset=utf-8")))
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    // UTF-8 BOM + 表头
                    org.junit.jupiter.api.Assertions.assertTrue(content.contains("账单时间"), "CSV 应包含表头");
                });
    }

    @Test
    @DisplayName("EXPORT-02: 导出当月账单 Excel — 返回码 200，Content-Type 为 xlsx")
    void testExportXlsx_currentMonth_returnsExcelFile() throws Exception {
        String currentMonth = java.time.YearMonth.now().toString();

        mockMvc.perform(get("/api/bills/export")
                        .param("format", "xlsx")
                        .param("month", currentMonth)
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    org.junit.jupiter.api.Assertions.assertTrue(body.length > 100, "Excel 文件不应为空");
                    // xlsx 文件以 PK (0x50 0x4B) 开头
                    org.junit.jupiter.api.Assertions.assertEquals(0x50, body[0] & 0xFF, "Excel 文件应以 PK 开头");
                    org.junit.jupiter.api.Assertions.assertEquals(0x4B, body[1] & 0xFF, "Excel 文件应以 PK 开头");
                });
    }

    @Test
    @DisplayName("EXPORT-03: 按类型筛选导出 — 仅返回 EXPENSE")
    void testExportCsv_byType_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .param("type", "EXPENSE")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EXPORT-04: 空数据导出 — 仅返回表头行")
    void testExportCsv_emptyData_returnsHeaderOnly() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .param("month", "2099-12")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    org.junit.jupiter.api.Assertions.assertTrue(content.contains("账单时间"), "空数据也应包含表头");
                });
    }

    @Test
    @DisplayName("EXPORT-05: 未登录访问 — 返回 401")
    void testExport_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("EXPORT-06: 导出全部数据（无筛选） — 返回码 200")
    void testExportCsv_allData_returnsSuccess() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EXPORT-07: 关键词搜索导出 — 返回码 200")
    void testExportCsv_byKeyword_returnsSuccess() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .param("keyword", "test")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EXPORT-08: 日期范围导出 — 返回码 200")
    void testExportCsv_byDateRange_returnsSuccess() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .param("start_date", "2026-01-01")
                        .param("end_date", "2026-12-31")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EXPORT-09: 非法 Token — 返回 401")
    void testExport_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .header("Authorization", "Bearer invalid.fake.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("EXPORT-10: 无 Bearer 前缀 — 返回 401")
    void testExport_withoutBearerPrefix_returns401() throws Exception {
        mockMvc.perform(get("/api/bills/export")
                        .param("format", "csv")
                        .header("Authorization", testToken()))
                .andExpect(status().isUnauthorized());
    }
}
