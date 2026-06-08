package com.xingzhewk.service.sms;

/**
 * 短信发送通道。
 *
 * 抽出来是为了把「业务逻辑（生成码 + 存储 + 频控）」和「具体怎么把码送到用户手机」分开：
 *   - dev / 个人项目阶段：{@code LogSmsProvider} 写日志，开发者自己看 console；
 *   - 接入阿里云/腾讯云：新增 AliyunSmsProvider 实现，业务代码一行不改；
 *   - prod 未接通道：{@code NoopSmsProvider} 直接拒绝，避免「假装能发」的安全洞。
 *
 * 注意：实现方不应该把 code 返回给前端 —— 这是验证码语义的底线。
 */
public interface SmsProvider {

    /**
     * 把验证码送达手机号。
     *
     * @return true 送达成功；false 通道未接入/被显式禁用，调用方应回退到「请联系管理员」
     * @throws RuntimeException 通道异常（如阿里云返回错误），由全局异常拦截转 5xx
     */
    boolean send(String phone, String code);

    /**
     * 通道名，用于日志和健康检查（actuator）。
     */
    String name();
}
