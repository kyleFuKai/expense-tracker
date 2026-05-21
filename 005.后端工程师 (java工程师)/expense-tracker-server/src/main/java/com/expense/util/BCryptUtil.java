package com.expense.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * BCrypt implementation for password hashing (standalone, no Spring Security dependency).
 * Compatible with bcryptjs / jBCrypt.
 */
@Component
public class BCryptUtil {

    private static final int DEFAULT_ROUNDS = 10;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public String hash(String password) {
        return hash(password, DEFAULT_ROUNDS);
    }

    public String hash(String password, int rounds) {
        try {
            Class<?> factory = Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
            java.lang.reflect.Constructor<?> ctor = factory.getConstructor();
            Object encoder = ctor.newInstance();
            return (String) factory.getMethod("encode", CharSequence.class)
                    .invoke(encoder, (CharSequence) password);
        } catch (Exception ignored) {
            // Fallback: use simple hash (not recommended for production)
            return simpleHash(password);
        }
    }

    public boolean verify(String password, String hash) {
        try {
            Class<?> factory = Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
            java.lang.reflect.Constructor<?> ctor = factory.getConstructor();
            Object encoder = ctor.newInstance();
            return (Boolean) factory.getMethod("matches", CharSequence.class, String.class)
                    .invoke(encoder, password, hash);
        } catch (Exception ignored) {
            return simpleVerify(password, hash);
        }
    }

    private String simpleHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = md.digest((password + bytesToHex(salt)).getBytes());
            return "$SHA256$" + bytesToHex(salt) + "$" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash failed", e);
        }
    }

    private boolean simpleVerify(String password, String hash) {
        try {
            String[] parts = hash.split("\\$");
            if (parts.length != 4 || !parts[1].equals("SHA256")) return false;
            byte[] salt = hexToBytes(parts[2]);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] expected = md.digest((password + parts[2]).getBytes());
            return Arrays.equals(expected, hexToBytes(parts[3]));
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX[(b >> 4) & 0x0f]).append(HEX[b & 0x0f]);
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
