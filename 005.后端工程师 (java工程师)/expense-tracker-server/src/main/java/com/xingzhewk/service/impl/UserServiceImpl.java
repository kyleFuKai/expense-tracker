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
import com.xingzhewk.service.sms.SmsProvider;
import com.xingzhewk.service.store.LoginAttemptStore;
import com.xingzhewk.service.store.SmsCodeStore;
import com.xingzhewk.util.JwtUtil;
import com.xingzhewk.util.PasswordUtil;
import com.xingzhewk.vo.LoginVO;
import com.xingzhewk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    /**
     * SecureRandom 一次实例化即可，本身线程安全。
     * 不用 ThreadLocalRandom 是因为这是安全敏感的码（被枚举=账号接管）。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 头像 MIME 白名单 */
    private static final Set<String> AVATAR_MIME_WHITELIST = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    /** 头像最大字节数：2 MiB（与 Node + 契约 x-constants.avatar.maxBytes 一致） */
    private static final long AVATAR_MAX_BYTES = 2L * 1024 * 1024;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final LoginAttemptStore loginAttemptStore;
    private final SmsCodeStore smsCodeStore;
    private final SmsProvider smsProvider;

    /**
     * 头像落盘目录。默认 ../finance/uploads/avatars/ —— 与 Node 后端共享同一目录。
     * 通过 app.upload.avatar-dir 可覆盖，未来切 NFS / 对象存储不需要改代码。
     */
    @Value("${app.upload.avatar-dir:../finance/uploads/avatars}")
    private String avatarDir;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<LoginVO> register(RegisterDTO dto) {
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

        // DIFFS #4：注册即登录，签发 JWT 一并返回
        String token = jwtUtil.generateToken(user.getId(), user.getPhone());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickname(nickname);

        log.info("用户注册成功, userId={}, phone={}", user.getId(), phone);
        return Result.success(vo);
    }

    @Override
    public Result<LoginVO> login(LoginDTO dto) {
        String phone = dto.getPhone().replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        // 锁定窗口由 store 内部按 TTL 维护，外层只看「还剩多久」
        long lockedMs = loginAttemptStore.lockedRemainingMs(phone);
        if (lockedMs > 0) {
            long remainingSeconds = (lockedMs + 999) / 1000; // 向上取整，不要给用户看到 0 秒
            throw new BusinessException(429, "登录失败次数过多，请 " + remainingSeconds + " 秒后再试");
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
        loginAttemptStore.clear(phone);

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
        int failedCount = loginAttemptStore.recordFailure(phone);
        log.warn("登录失败, phone={}, 失败次数={}", phone, failedCount);
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

        // 先做频控（用临时占位 code），避免短信通道被刷
        // 真正的 code 留到下一步生成 —— 频控失败时不浪费熵
        String code = generateSmsCode();

        boolean stored = smsCodeStore.putIfNotThrottled(
                phone, code, SMS_CODE_EXPIRY_SECONDS, SMS_CODE_SEND_INTERVAL_MS);
        if (!stored) {
            throw new BusinessException(429, "发送过于频繁，请稍后再试");
        }

        // 走通道下发。送达失败（如 NoopSmsProvider）就回滚存储，避免出现「码已存但用户收不到」
        boolean delivered;
        try {
            delivered = smsProvider.send(phone, code);
        } catch (RuntimeException ex) {
            smsCodeStore.remove(phone);
            log.error("短信发送异常 provider={} phone={}", smsProvider.name(), phone, ex);
            throw new BusinessException(503, "短信服务暂不可用，请稍后再试");
        }
        if (!delivered) {
            smsCodeStore.remove(phone);
            throw new BusinessException(503, "短信功能暂未开放，请联系管理员重置密码");
        }

        log.info("短信验证码已发送, provider={}, phone={}", smsProvider.name(), phone);
        return Result.success();
    }

    /**
     * 生成 6 位数字验证码。
     *
     * 用 SecureRandom 而不是 ThreadLocalRandom：验证码的安全模型就是「攻击者
     * 拿不到 → 拿不到账号」，弱随机源会让 100 万的空间被预测攻击大幅缩小。
     */
    private String generateSmsCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
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

        String storedCode = smsCodeStore.get(phone);
        if (storedCode == null) {
            throw new BusinessException(400, "验证码已过期");
        }
        if (!storedCode.equals(dto.getSmsCode())) {
            throw new BusinessException(400, "验证码错误");
        }

        user.setPasswordHash(passwordUtil.hash(newPassword));
        userMapper.updateById(user);
        smsCodeStore.remove(phone);
        log.info("密码重置成功, phone={}", phone);
        return Result.success();
    }

    // ===== DIFFS #5: 补齐 Node 端独有的 3 个用户端点 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> bindPhone(Long userId, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(400, "手机号不能为空");
        }
        String normalized = phone.replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }

        // 防重：同手机号已被其他账号绑定
        Long conflict = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, normalized)
                .ne(User::getId, userId));
        if (conflict > 0) {
            throw new BusinessException(409, "该手机号已被其他账号绑定");
        }

        User update = new User();
        update.setId(userId);
        update.setPhone(normalized);
        userMapper.updateById(update);
        log.info("绑定手机号成功, userId={}, phone={}", userId, normalized);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unbindPhone(Long userId) {
        // 与 Node 一致：phone 置空、country_code 重置为 +86
        User update = new User();
        update.setId(userId);
        update.setPhone("");
        update.setCountryCode("+86");
        userMapper.updateById(update);
        log.info("解绑手机号成功, userId={}", userId);
        return Result.success();
    }

    @Override
    public Result<Map<String, String>> uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "未选择文件");
        }
        if (file.getSize() > AVATAR_MAX_BYTES) {
            throw new BusinessException(400, "头像大小不能超过 2 MiB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !AVATAR_MIME_WHITELIST.contains(contentType.toLowerCase())) {
            throw new BusinessException(400, "仅支持图片格式（jpg/png/gif/webp）");
        }

        // 文件名：user_<uid>_<ts>.<ext>，与 Node 命名一致
        String ext = extractExtension(file.getOriginalFilename(), contentType);
        String filename = "user_" + userId + "_" + System.currentTimeMillis() + ext;
        Path dir = Paths.get(avatarDir).toAbsolutePath().normalize();
        Path target = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("保存头像失败 userId={} target={}", userId, target, e);
            throw new BusinessException(500, "保存头像失败");
        }

        // URL 走 /uploads/avatars/，由 WebConfig.addResourceHandlers 映射到 avatarDir
        String url = "/uploads/avatars/" + filename;
        User update = new User();
        update.setId(userId);
        update.setAvatarUrl(url);
        userMapper.updateById(update);
        log.info("头像上传成功, userId={}, url={}", userId, url);
        return Result.success(Map.of("url", url));
    }

    /**
     * 优先从原文件名取扩展名，缺失时按 MIME 兜底。避免恶意上传 `evil.exe` 后写入磁盘。
     */
    private static String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot).toLowerCase();
                // 只接受图片扩展名
                if (Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(ext)) {
                    return ext;
                }
            }
        }
        // MIME 兜底
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }
}
