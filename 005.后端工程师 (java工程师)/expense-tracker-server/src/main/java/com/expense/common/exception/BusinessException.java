package com.expense.common.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * 用于 Service 层抛出业务级别的错误（如资源不存在、参数冲突等），
 * 由 GlobalExceptionHandler 统一拦截并转换为 Result 响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码（对应 HTTP 响应 code） */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
