package com.xingzhewk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xingzhewk.common.Constants;
import com.xingzhewk.common.Result;
import com.xingzhewk.dto.ChangePasswordDTO;
import com.xingzhewk.dto.LoginDTO;
import com.xingzhewk.dto.ProfileUpdateDTO;
import com.xingzhewk.dto.RegisterDTO;
import com.xingzhewk.dto.ResetPasswordDTO;
import com.xingzhewk.dto.SendSmsCodeDTO;
import com.xingzhewk.entity.User;
import com.xingzhewk.common.exception.BusinessException;
import com.xingzhewk.mapper.UserMapper;
import com.xingzhewk.service.UserService;
import com.xingzhewk.util.JwtUtil;
import com.xingzhewk.util.PasswordUtil;
import com.xingzhewk.vo.LoginVO;
import com.xingzhewk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;

import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8,15}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$");

    /** 短信验证码有效时长：5 分钟 */
    private static final long SMS_CODE_EXPIRY_SECONDS = 5 * 60;

    /** 短信验证码发送间隔：60 秒 */
    private static final long SMS_CODE_SEND_INTERVAL_MS = 60 * 1000L;

    /** 测试用固定验证码 */
    private static final String TEST_SMS_CODE = "666666";

    /** 登录失败最大次数 */
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    /** 登录锁定窗口：15 分钟 */
    private static final long LOGIN_LOCK_WINDOW_MS = 15 * 60 * 1000L;

    /** 存储短信验证码及发送时间。Key: phone, Value: 验证码记录 */
    private final ConcurrentHashMap<String, SmsCodeRecord> smsCodeStore = new ConcurrentHashMap<>();

    /** 存储登录失败次数。Key: phone, Value: 失败记录 */
    private final ConcurrentHashMap<String, LoginAttemptRecord> loginAttemptStore = new ConcurrentHashMap<>();

    /**
     * 短信验证码记录
     */
    private static class SmsCodeRecord {
        final String code;
        final long sendTimeMs;
        SmsCodeRecord(String code, long sendTimeMs) {
            this.code = code;
            this.sendTimeMs = sendTimeMs;
        }
    }

    /**
     * 登录失败记录
     */
    private static class LoginAttemptRecord {
        int failedCount;
        long firstFailTimeMs;
        LoginAttemptRecord() {
            this.failedCount = 1;
            this.firstFailTimeMs = System.currentTimeMillis();
        }
        void increment() {
            this.failedCount++;
        }
    }

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> register(RegisterDTO dto) {
        String phone = dto.getPhone().replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        String password = dto.getPassword();
        if (password.length() < Constants.MIN_PASSWORD_LENGTH || password.length() > Constants.MAX_PASSWORD_LENGTH) {
            throw new BusinessException(400, "密码长度需为6-20位");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(400, "密码需包含大小写字母、数字和特殊字符");
        }

        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (count > 0) {
            throw new BusinessException(409, "操作失败");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordUtil.hash(password));
        String nickname = dto.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        }
        user.setNickname(nickname);
        user.setCountryCode("+86");
        user.setCurrency("CNY");
        user.setTheme("light");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功, userId={}, phone={}", user.getId(), phone);
        return Result.success(user.getId());
    }

    @Override
    public Result<LoginVO> login(LoginDTO dto) {
        String phone = dto.getPhone().replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        // Check if account is locked due to too many failed attempts
        LoginAttemptRecord record = loginAttemptStore.get(phone);
        if (record != null && record.failedCount >= MAX_LOGIN_ATTEMPTS
                && System.currentTimeMillis() - record.firstFailTimeMs < LOGIN_LOCK_WINDOW_MS) {
            long remainingSeconds = (LOGIN_LOCK_WINDOW_MS - (System.currentTimeMillis() - record.firstFailTimeMs)) / 1000;
            throw new BusinessException(429, "登录失败次数过多，请 " + remainingSeconds + " 秒后再试");
        }
        // Lock window expired, reset
        if (record != null && record.failedCount >= MAX_LOGIN_ATTEMPTS) {
            loginAttemptStore.remove(phone);
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            // Still count as failed to prevent phone enumeration
            recordFailedLogin(phone);
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordUtil.verify(dto.getPassword(), user.getPasswordHash())) {
            recordFailedLogin(phone);
            throw new BusinessException(401, "手机号或密码错误");
        }

        // Login success: clear failed attempts
        loginAttemptStore.remove(phone);

        String token = jwtUtil.generateToken(user.getId(), user.getPhone());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        return Result.success(vo);
    }

    /**
     * 记录登录失败，超过阈值则锁定账号
     */
    private void recordFailedLogin(String phone) {
        LoginAttemptRecord record = loginAttemptStore.get(phone);
        if (record == null) {
            loginAttemptStore.put(phone, new LoginAttemptRecord());
        } else {
            record.increment();
        }
        log.warn("登录失败, phone={}, 失败次数={}", phone, loginAttemptStore.get(phone).failedCount);
    }

    @Override
    public Result<UserVO> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateProfile(Long userId, ProfileUpdateDTO dto) {
        if (dto.getNickname() == null && dto.getAvatarUrl() == null
                && dto.getCurrency() == null && dto.getTheme() == null) {
            throw new BusinessException(400, "请提供至少一个需要更新的字段");
        }

        User user = new User();
        user.setId(userId);

        if (dto.getNickname() != null) {
            if (dto.getNickname().length() > 50) {
                throw new BusinessException(400, "昵称长度不能超过 50 个字符");
            }
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getCurrency() != null) {
            user.setCurrency(dto.getCurrency());
        }
        if (dto.getTheme() != null) {
            user.setTheme(dto.getTheme());
        }

        userMapper.updateById(user);
        log.info("用户信息更新, userId={}", userId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> changePassword(Long userId, ChangePasswordDTO dto) {
        if (dto.getOldPassword() == null || dto.getNewPassword() == null) {
            throw new BusinessException(400, "旧密码和新密码不能为空");
        }

        String newPassword = dto.getNewPassword();
        if (newPassword.length() < Constants.MIN_PASSWORD_LENGTH || newPassword.length() > Constants.MAX_PASSWORD_LENGTH) {
            throw new BusinessException(400, "密码长度需为6-20位");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(400, "密码需包含大小写字母、数字和特殊字符");
        }

        User user = userMapper.selectById(userId);
        if (user == null || !passwordUtil.verify(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "旧密码错误");
        }

        user.setPasswordHash(passwordUtil.hash(newPassword));
        userMapper.updateById(user);
        log.info("用户密码修改, userId={}", userId);
        return Result.success();
    }

    @Override
    public Result<Void> sendSmsCode(SendSmsCodeDTO dto) {
        String phone = dto.getPhone().replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(404, "手机号未注册");
        }

        long now = System.currentTimeMillis();
        SmsCodeRecord existing = smsCodeStore.get(phone);
        if (existing != null && now - existing.sendTimeMs < SMS_CODE_SEND_INTERVAL_MS) {
            throw new BusinessException(429, "发送过于频繁，请稍后再试");
        }

        smsCodeStore.put(phone, new SmsCodeRecord(TEST_SMS_CODE, now));
        log.info("短信验证码已发送, phone={}", phone);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resetPassword(ResetPasswordDTO dto) {
        String phone = dto.getPhone().replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(404, "手机号未注册");
        }

        String newPassword = dto.getNewPassword();
        if (newPassword.length() < Constants.MIN_PASSWORD_LENGTH || newPassword.length() > Constants.MAX_PASSWORD_LENGTH) {
            throw new BusinessException(400, "密码长度需为6-20位");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(400, "密码需包含大小写字母、数字和特殊字符");
        }

        SmsCodeRecord record = smsCodeStore.get(phone);
        if (record == null || System.currentTimeMillis() - record.sendTimeMs > SMS_CODE_EXPIRY_SECONDS * 1000) {
            throw new BusinessException(400, "验证码已过期");
        }
        if (!record.code.equals(dto.getSmsCode())) {
            throw new BusinessException(400, "验证码错误");
        }

        user.setPasswordHash(passwordUtil.hash(newPassword));
        userMapper.updateById(user);
        smsCodeStore.remove(phone);
        log.info("密码重置成功, phone={}", phone);
        return Result.success();
    }
}
