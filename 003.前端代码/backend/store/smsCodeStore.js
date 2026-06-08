/**
 * 短信验证码存储。
 *
 * 与 Java 后端 {@code com.xingzhewk.service.store.SmsCodeStore} 接口对齐。
 * 详见 contracts/DIFFS.md #2 #3。
 *
 * 仅内存实现（V1.1 不上 Redis，与 Java dev 默认一致）。
 */
class InMemorySmsCodeStore {
    constructor({ cleanupIntervalMs = 60 * 1000 } = {}) {
        this.store = new Map();
        this._cleanupTimer = setInterval(() => this.cleanupExpired(), cleanupIntervalMs);
        if (this._cleanupTimer.unref) this._cleanupTimer.unref();
    }

    /**
     * 写入一条新验证码。若距上次写入小于 minIntervalMs 返回 false（频控）。
     */
    putIfNotThrottled(key, code, ttlSeconds, minIntervalMs) {
        const now = Date.now();
        const prev = this.store.get(key);
        if (prev && now - prev.writeTimeMs < minIntervalMs) {
            return false;
        }
        this.store.set(key, {
            code,
            writeTimeMs: now,
            expireAtMs: now + ttlSeconds * 1000
        });
        return true;
    }

    /** 已过期或不存在返回 null。 */
    get(key) {
        const r = this.store.get(key);
        if (!r) return null;
        if (Date.now() > r.expireAtMs) {
            this.store.delete(key);
            return null;
        }
        return r.code;
    }

    /** 校验后删除（一次性消费）。 */
    remove(key) {
        this.store.delete(key);
    }

    cleanupExpired() {
        const now = Date.now();
        for (const [k, v] of this.store.entries()) {
            if (now > v.expireAtMs) this.store.delete(k);
        }
    }

    size() { return this.store.size; }

    destroy() {
        if (this._cleanupTimer) clearInterval(this._cleanupTimer);
        this._cleanupTimer = null;
    }
}

module.exports = { InMemorySmsCodeStore };
