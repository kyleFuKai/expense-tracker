/**
 *   cd 003.前端代码/backend && node store/smsCodeStore.test.js
 */
const assert = require('node:assert/strict');
const { InMemorySmsCodeStore } = require('./smsCodeStore');

let passed = 0;
async function test(name, fn) {
    try { await fn(); passed++; console.log('  ✓ ' + name); }
    catch (e) { console.error('  ✗ ' + name + '\n    ' + (e.stack || e.message)); process.exitCode = 1; }
}

(async () => {
    console.log('smsCodeStore');

    await test('首次发送成功', () => {
        const s = new InMemorySmsCodeStore();
        assert.equal(s.putIfNotThrottled('a', '111111', 300, 60_000), true);
        assert.equal(s.get('a'), '111111');
        s.destroy();
    });

    await test('间隔内重复发送被拒', () => {
        const s = new InMemorySmsCodeStore();
        assert.equal(s.putIfNotThrottled('a', '111111', 300, 60_000), true);
        assert.equal(s.putIfNotThrottled('a', '222222', 300, 60_000), false);
        assert.equal(s.get('a'), '111111', '旧码保留');
        s.destroy();
    });

    await test('超过间隔后允许再发', async () => {
        const s = new InMemorySmsCodeStore();
        assert.equal(s.putIfNotThrottled('a', '111111', 300, 30), true);
        await new Promise(r => setTimeout(r, 50));
        assert.equal(s.putIfNotThrottled('a', '222222', 300, 30), true);
        assert.equal(s.get('a'), '222222');
        s.destroy();
    });

    await test('ttl 过期后取不到', async () => {
        const s = new InMemorySmsCodeStore();
        s.putIfNotThrottled('a', '111111', 0, 0); // ttlSeconds=0 等同立即过期
        await new Promise(r => setTimeout(r, 20));
        assert.equal(s.get('a'), null);
        assert.equal(s.size(), 0);
        s.destroy();
    });

    await test('remove 一次性', () => {
        const s = new InMemorySmsCodeStore();
        s.putIfNotThrottled('a', '111111', 300, 0);
        s.remove('a');
        assert.equal(s.get('a'), null);
        s.destroy();
    });

    await test('cleanupExpired 清过期', async () => {
        const s = new InMemorySmsCodeStore();
        s.putIfNotThrottled('a', '1', 0, 0);
        s.putIfNotThrottled('b', '2', 0, 0);
        await new Promise(r => setTimeout(r, 20));
        s.cleanupExpired();
        assert.equal(s.size(), 0);
        s.destroy();
    });

    await test('不同手机号互不干扰', () => {
        const s = new InMemorySmsCodeStore();
        assert.equal(s.putIfNotThrottled('a', '111111', 300, 60_000), true);
        assert.equal(s.putIfNotThrottled('b', '222222', 300, 60_000), true,
            'a 的频控不应阻止 b');
        assert.equal(s.get('a'), '111111');
        assert.equal(s.get('b'), '222222');
        s.destroy();
    });

    console.log(`\n${passed} passed`);
})();
