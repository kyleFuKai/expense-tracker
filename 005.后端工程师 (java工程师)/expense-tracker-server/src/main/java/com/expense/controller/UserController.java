package com.expense.controller;

import com.expense.common.Result;
import com.expense.dto.ChangePasswordDTO;
import com.expense.dto.ProfileUpdateDTO;
import com.expense.service.UserService;
import com.expense.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * 处理用户信息获取/修改、密码修改等接口。
 * 所有接口均需 JWT 认证（由 JwtInterceptor 校验）。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息（昵称、头像、手机、币种、主题等）
     */
    @GetMapping("/profile")
    public Result<UserVO> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.getProfile(userId);
    }

    /**
     * 更新当前登录用户信息
     *
     * @param dto 更新参数，可选：nickname、avatarUrl、currency（CNY/USD/EUR）、theme（light/dark），至少提供一项
     * @return 空，code=400 表示未提供任何更新字段
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(HttpServletRequest request, @Valid @RequestBody ProfileUpdateDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.updateProfile(userId, dto);
    }

    /**
     * 修改密码
     *
     * @param dto 包含 oldPassword（原密码）、newPassword（新密码，6-20位，含大小写字母+数字+特殊字符）
     * @return 空，code=401 表示旧密码错误
     */
    @PutMapping("/password")
    public Result<Void> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.changePassword(userId, dto);
    }
}
