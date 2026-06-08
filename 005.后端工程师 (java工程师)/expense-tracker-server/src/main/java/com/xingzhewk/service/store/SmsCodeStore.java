package com.xingzhewk.service.store;

/**
 * 短信验证码存储。
 *
 * 抽象同 {@link LoginAttemptStore}：内存实现用于 dev/单机，
 * Redis 实现用于集群部署。所有实现需保证：
 *   - 写入时按 TTL 自动过期；
 *   - 支持「发送间隔」语义（距上次写入小于 minIntervalMs 时拒绝写入）。
 */
public interface SmsCodeStore {

    /**
     * 写入一条新验证码。若距上次写入小于 minIntervalMs，返回 false 表示频控拒绝。
     *
     * @param key           手机号
     * @param code          验证码
     * @param ttlSeconds    存活时长
     * @param minIntervalMs 最小发送间隔
     * @return true 写入成功；false 被频控拦截
     */
    boolean putIfNotThrottled(String key, String code, long ttlSeconds, long minIntervalMs);

    /**
     * 取验证码。已过期或不存在返回 null。
     */
    String get(String key);

    /**
     * 校验后删除（一次性）。
     */
    void remove(String key);
}
