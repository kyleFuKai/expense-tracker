package com.expense.service;

import com.expense.common.Result;
import com.expense.dto.ChangePasswordDTO;
import com.expense.dto.LoginDTO;
import com.expense.dto.ProfileUpdateDTO;
import com.expense.dto.RegisterDTO;
import com.expense.dto.ResetPasswordDTO;
import com.expense.dto.SendSmsCodeDTO;
import com.expense.vo.LoginVO;
import com.expense.vo.UserVO;

/**
 * 用户服务接口
 *
 * 处理用户注册、登录、个人信息管理、密码修改等业务逻辑。
 */
public interface UserService {

    /**
     * 手机号注册
     *
     * @param dto 注册参数（phone、password、nickname）
     * @return {id: 用户ID}，code=409 表示手机号已注册
     */
    Result<Long> register(RegisterDTO dto);

    /**
     * 手机号密码登录
     *
     * @param dto 登录参数（phone、password）
     * @return {token, userId, nickname}，code=401 表示认证失败
     */
    Result<LoginVO> login(LoginDTO dto);

    /**
     * 获取当前用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息（昵称、头像、手机、币种、主题等）
     */
    Result<UserVO> getProfile(Long userId);

    /**
     * 更新当前用户信息
     *
     * @param userId 用户 ID
     * @param dto 更新参数（nickname、avatarUrl、currency、theme），至少提供一项
     * @return 空，code=400 表示未提供任何更新字段
     */
    Result<Void> updateProfile(Long userId, ProfileUpdateDTO dto);

    /**
     * 修改密码
     *
     * @param userId 用户 ID
     * @param dto 密码参数（oldPassword、newPassword）
     * @return 空，code=401 表示旧密码错误
     */
    Result<Void> changePassword(Long userId, ChangePasswordDTO dto);

    /**
     * 发送短信验证码（用于忘记密码）
     *
     * @param dto 发送参数（phone）
     * @return 空，code=404 表示手机号未注册，code=429 表示发送过于频繁
     */
    Result<Void> sendSmsCode(SendSmsCodeDTO dto);

    /**
     * 重置密码（通过短信验证码验证身份）
     *
     * @param dto 重置参数（phone、smsCode、newPassword）
     * @return 空，code=400 表示验证码错误或过期，code=404 表示手机号未注册
     */
    Result<Void> resetPassword(ResetPasswordDTO dto);
}
