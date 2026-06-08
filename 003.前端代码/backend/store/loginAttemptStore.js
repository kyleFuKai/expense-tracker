/**
 * 登录失败计数存储。
 *
 * 与 Java 后端 {@code com.xingzhewk.service.store.LoginAttemptStore} 接口对齐。
 * 详见 contracts/DIFFS.md #1。
 *
 * 仅内存实现（V1.1 不上 Redis，与 Java dev 默认一致）。
 * 多实例部署需迁移到 Redis —— 见 InMemorySmsCodeStore 注释。
 */
class InMemoryLoginAttemptStore {
    constructor({ windowMs = 15 * 60 * 1000, maxAttempts = 5, cleanupIntervalMs = 60 * 1000 } = {}) {
        this.windowMs = windowMs;
        this.maxAttempts = maxAttempts;
        this.store = new Map();
        // 兜底清理：每分钟扫一遍，过期条目及时删除，防止长期运行的内存泄漏
        this._cleanupTimer = setInterval(() => this.cleanupExpired(), cleanupIntervalMs);
        if (this._cleanupTimer.unref) this._cleanupTimer.unref();
    }

    /** 还在锁定期则返回剩余毫秒数；否则 0。 */
    lockedRemainingMs(key) {
        const r = this.store.get(key);
        if (!r || r.failedCount < this.maxAttempts) return 0;
        const elapsed = Date.now() - r.firstFailTimeMs;
        if (elapsed >= this.windowMs) {
            this.store.delete(key);
            return 0;
        }
        return this.windowMs - elapsed;
    }

    /** 失败 +1，返回当前次数。 */
    recordFailure(key) {
        const now = Date.now();
        const prev = this.store.get(key);
        const next = (!prev || now - prev.firstFailTimeMs >= this.windowMs)
            ? { failedCount: 1, firstFailTimeMs: now }
            : { failedCount: prev.failedCount + 1, firstFailTimeMs: prev.firstFailTimeMs };
        this.store.set(key, next);
        return next.failedCount;
    }

    /** 登录成功清空。 */
    clear(key) {
        this.store.delete(key);
    }

    /** 定时清理过期的 key。Map 不会自动回收，永远不活跃的 key 也要清。 */
    cleanupExpired() {
        const now = Date.now();
        for (const [k, v] of this.store.entries()) {
            if (now - v.firstFailTimeMs >= this.windowMs) {
                this.store.delete(k);
            }
        }
    }

    size() { return this.store.size; }

    /** 测试 / 进程退出时手动调，避免 jest 报 "open handle"。 */
    destroy() {
        if (this._cleanupTimer) clearInterval(this._cleanupTimer);
        this._cleanupTimer = null;
    }
}

module.exports = { InMemoryLoginAttemptStore };
