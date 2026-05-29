package com.xingzhewk.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码加密工具（基于 Spring Security PasswordEncoder）。
 * 提供与旧版 BCryptUtil 兼容的方法签名。
 */
@Component
public class PasswordUtil {

    private final PasswordEncoder encoder;

    public PasswordUtil() {
        this(new BCryptPasswordEncoder());
    }

    public PasswordUtil(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * 对明文密码进行 BCrypt 加密。
     */
    public String hash(String password) {
        return encoder.encode(password);
    }

    /**
     * 校验明文密码与哈希值是否匹配。
     */
    public boolean verify(String password, String hash) {
        return encoder.matches(password, hash);
    }
}
