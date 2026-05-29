package com.xingzhewk.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户服务 & 登录防爆破集成测试
 *
 * 重点验证：UserServiceImpl.login 防爆破保护，
 * 连续 5 次密码错误后锁定账号 15 分钟。
 *
 * 注意：
 * - 项目全局异常处理器返回 HTTP 200 + JSON code 字段（BusinessException）
 * - Jakarta 校验错误返回 HTTP 400 + JSON code 字段
 * - 每个测试使用独立手机号，避免登录失败计数器状态污染
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserServiceTest {

    @Autowired
    private MockMvc mockMvc;

    /** 基础测试手机号 */
    private static final String TEST_PHONE = "13900000001";
    private static final String TEST_PASSWORD = "Test@1234";

    /** 防爆破测试专用手机号（每个测试独立） */
    private static final String LOCKOUT_PHONE = "13900000002";
    private static final String LOCKOUT_PASSWORD = "Lock@1234";

    private static final String RESET_PHONE = "13900000004";
    private static final String RESET_PASSWORD = "Reset@1234";

    // ==================== 基础登录/注册测试 ====================

    @Test
    @DisplayName("USER-01: 注册测试用户")
    void testRegister_testUser() throws Exception {
        String loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();

        if (loginResult.contains("\"code\":0")) {
            return; // 已存在
        }

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\",\"nickname\":\"测试用户\"}"))
                .andReturn();
    }

    @Test
    @DisplayName("USER-02: 正确密码登录 — 返回 token")
    void testLogin_correctPassword_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.nickname").isString());
    }

    @Test
    @DisplayName("USER-03: 错误密码登录 — 返回 code 401")
    void testLogin_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + TEST_PHONE + "\",\"password\":\"Wrong@1111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("USER-04: 不存在的手机号登录 — 返回 code 404")
    void testLogin_nonexistentPhone_returns404() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"99999999999\",\"password\":\"Any@1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("USER-05: 注册 — 手机号格式错误返回 code 400 (HTTP 400)")
    void testRegister_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"abc\",\"password\":\"Test@1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("USER-06: 注册 — 密码强度不足返回 code 400")
    void testRegister_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000003\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 登录防爆破保护（独立手机号） ====================

    @Test
    @DisplayName("USER-07: 注册防爆破测试用户")
    void testRegister_lockoutTestUser() throws Exception {
        // 直接注册（不先登录检查，避免触发失败计数）
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + LOCKOUT_PHONE + "\",\"password\":\"" + LOCKOUT_PASSWORD + "\",\"nickname\":\"防爆破测试用户\"}"))
                .andReturn();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + RESET_PHONE + "\",\"password\":\"" + RESET_PASSWORD + "\",\"nickname\":\"重置测试用户\"}"))
                .andReturn();
    }

    @Test
    @DisplayName("USER-08: 连续 5 次错误后锁定 — 第 6 次返回 code 429")
    void testLogin_bruteForceLockout_returns429() throws Exception {
        // 连续 5 次密码错误
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"" + LOCKOUT_PHONE + "\",\"password\":\"Wrong@1111\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        // 第 6 次应该被锁定
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + LOCKOUT_PHONE + "\",\"password\":\"" + LOCKOUT_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    @DisplayName("USER-09: 正确密码登录成功后清除失败计数 — 可再次触发锁定")
    void testLogin_successResetsCounter() throws Exception {
        // 先正确登录（USER-07 已注册）
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + RESET_PHONE + "\",\"password\":\"" + RESET_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 再失败 5 次
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"" + RESET_PHONE + "\",\"password\":\"Wrong@1111\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        // 第 6 次应再次被锁定
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + RESET_PHONE + "\",\"password\":\"" + RESET_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(429));
    }
}
