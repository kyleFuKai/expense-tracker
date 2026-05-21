package com.expense.controller;

import com.expense.common.Result;
import com.expense.dto.LoginDTO;
import com.expense.dto.RegisterDTO;
import com.expense.dto.ResetPasswordDTO;
import com.expense.dto.SendSmsCodeDTO;
import com.expense.service.UserService;
import com.expense.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 处理用户注册/登录等认证相关接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 手机号注册
     *
     * @param dto 注册参数，包含 phone（11位手机号）、password（6-20位，含大小写字母+数字+特殊字符）、nickname（昵称）
     * @return 用户ID，code=409 表示手机号已注册
     */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    /**
     * 手机号密码登录
     *
     * @param dto 登录参数，包含 phone、password
     * @return token + userId + nickname，code=401 表示手机号或密码错误
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * 发送短信验证码（用于忘记密码）
     *
     * @param dto 发送参数，包含 phone
     * @return 空，code=404 表示手机号未注册，code=429 表示发送过于频繁
     */
    @PostMapping("/send-sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeDTO dto) {
        return userService.sendSmsCode(dto);
    }

    /**
     * 重置密码
     *
     * @param dto 重置参数，包含 phone、smsCode、newPassword
     * @return 空，code=400 表示验证码错误或过期，code=404 表示手机号未注册
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        return userService.resetPassword(dto);
    }
}
