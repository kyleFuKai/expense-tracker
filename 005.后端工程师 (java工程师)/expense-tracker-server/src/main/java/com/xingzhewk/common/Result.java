package com.xingzhewk.common;

import lombok.Data;

/**
 * 统一响应封装类
 *
 * 所有接口返回统一格式：{code, msg, data}
 * code=0 表示成功，非 0 表示错误
 */
@Data
public class Result<T> {

    /** 响应码（0=成功，400=参数错误，401=未认证，404=不存在，409=冲突，500=服务器错误） */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    /**
     * 错误响应
     */
    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
