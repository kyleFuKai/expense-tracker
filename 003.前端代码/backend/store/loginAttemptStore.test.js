/**
 * 跟 billQuery.test.js 风格保持一致：零依赖，node 直接跑。
 *   cd 003.前端代码/backend && node store/loginAttemptStore.test.js
 *
 * 用 node:test 风格：把用例都写成 async，不用自己维护 passed 计数。
 */
const assert = require('node:assert/strict');
const { InMemoryLoginAttemptStore } = require('./loginAttemptStore');

let passed = 0;
async function test(name, fn) {
    try { await fn(); passed++; console.log('  ✓ ' + name); }
    catch (e) { console.error('  ✗ ' + name + '\n    ' + (e.stack || e.message)); process.exitCode = 1; }
}

(async () => {
    console.log('loginAttemptStore');

    await test('初始未锁定', () => {
        const s = new InMemoryLoginAttemptStore({ windowMs: 900_000, maxAttempts: 5 });
        assert.equal(s.lockedRemainingMs('13800000000'), 0);
        s.destroy();
    });

    await test('失败计数递增', () => {
        const s = new InMemoryLoginAttemptStore({ windowMs: 900_000, maxAttempts: 5 });
        assert.equal(s.recordFailure('a'), 1);
        assert.equal(s.recordFailure('a'), 2);
        assert.equal(s.recordFailure('a'), 3);
        s.destroy();
    });

    await test('达到阈值后进入锁定', () => {
        const s = new InMemoryLoginAttemptStore({ windowMs: 900_000, maxAttempts: 3 });
        s.recordFailure('a'); s.recordFailure('a');
        assert.equal(s.lockedRemainingMs('a'), 0, '未达阈值不锁');
        s.recordFailure('a');
        const remaining = s.lockedRemainingMs('a');
        assert.ok(remaining > 0 && remaining <= 900_000, '应锁定: ' + remaining);
        s.destroy();
    });

    await test('clear 后重置', () => {
        const s = new InMemoryLoginAttemptStore({ windowMs: 900_000, maxAttempts: 2 });
        s.recordFailure('a'); s.recordFailure('a');
        assert.ok(s.lockedRemainingMs('a') > 0);
        s.clear('a');
        assert.equal(s.lockedRemainingMs('a'), 0);
        assert.equal(s.recordFailure('a'), 1, 'clear 后下次失败从 1 开始');
        s.destroy();
    });

    await test('窗口期过后自动失效', async () => {
        const s = new InMemoryLoginAttemptStore({ windowMs: 50, maxAttempts: 2 });
        s.recordFailure('a'); s.recordFailure('a');
        assert.ok(s.lockedRemainingMs('a') > 0);
        await new Promise(r => setTimeout(r, 80));
        assert.equal(s.lockedRemainingMs('a'), 0);
        assert.equal(s.recordFailure('a'), 1, '新窗口从 1 开始');
        s.destroy();
    });

    await test('cleanupExpired 清过期', async () => {
        const s = new InMemoryLoginAttemptStore({ windowMs: 30, maxAttempts: 99 });
        s.recordFailure('a');
        s.recordFailure('b');
        assert.equal(s.size(), 2);
        await new Promise(r => setTimeout(r, 50));
        s.cleanupExpired();
        assert.equal(s.size(), 0);
        s.destroy();
    });

    await test('不同 key 独立计数', () => {
        const s = new InMemoryLoginAttemptStore({ windowMs: 900_000, maxAttempts: 5 });
        s.recordFailure('a'); s.recordFailure('a');
        s.recordFailure('b');
        assert.equal(s.recordFailure('a'), 3);
        assert.equal(s.recordFailure('b'), 2);
        s.destroy();
    });

    console.log(`\n${passed} passed`);
})();
