package com.xingzhewk.service.store;

/**
 * 登录失败计数存储。
 *
 * 抽象出来是为了在「单机内存」「Redis 集群」两种部署形态间切换。
 * 旧实现把 ConcurrentHashMap 直接放在 UserServiceImpl 里，
 *   1) 多实例部署时各自计数，会被绕过；
 *   2) 没有淘汰机制，长时间运行会泄漏。
 *
 * 实现需自行处理 TTL（窗口期之外的记录自动失效）。
 */
public interface LoginAttemptStore {

    /**
     * 当前是否处于锁定状态（已超过阈值且仍在窗口内）。
     *
     * @return 剩余锁定毫秒数；未锁定返回 0
     */
    long lockedRemainingMs(String key);

    /**
     * 失败 +1。返回当前失败次数；若已超过阈值，自动按窗口期持续锁定。
     */
    int recordFailure(String key);

    /**
     * 成功登录后清空。
     */
    void clear(String key);
}
