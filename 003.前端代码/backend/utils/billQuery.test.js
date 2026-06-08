/**
 * billQuery 单元测试（无测试框架依赖，直接 node 运行）
 *   cd 003.前端代码/backend && node utils/billQuery.test.js
 *
 * 用例覆盖：
 *   - 别名前缀正确应用
 *   - 参数顺序与 ? 占位符顺序严格一致
 *   - 空 filter 不产生 WHERE
 *   - appendCondition 在空/非空 where 上都能工作
 *   - 旧实现的脆弱点（多个 user_id = ? 字面量）不再误伤
 */
const assert = require('node:assert/strict');
const { buildBillWhere, appendCondition } = require('./billQuery');

let passed = 0;
function test(name, fn) {
    try {
        fn();
        passed++;
        console.log('  ✓ ' + name);
    } catch (err) {
        console.error('  ✗ ' + name);
        console.error('    ' + err.message);
        process.exitCode = 1;
    }
}

console.log('billQuery');

test('alias=b 给每个列加 b. 前缀', () => {
    const r = buildBillWhere(
        { userId: 1, month: '2026-05', type: 'expense' },
        { alias: 'b' }
    );
    assert.equal(
        r.where,
        'WHERE b.user_id = ? AND DATE_FORMAT(b.bill_time, "%Y-%m") = ? AND b.type = ?'
    );
    assert.deepEqual(r.params, [1, '2026-05', 'EXPENSE']);
});

test('无 alias 不加前缀', () => {
    const r = buildBillWhere({ userId: 7, month: '2026-06' });
    assert.equal(r.where, 'WHERE user_id = ? AND DATE_FORMAT(bill_time, "%Y-%m") = ?');
    assert.deepEqual(r.params, [7, '2026-06']);
});

test('只传 userId 的最小情况', () => {
    const r = buildBillWhere({ userId: 42 }, { alias: 'b' });
    assert.equal(r.where, 'WHERE b.user_id = ?');
    assert.deepEqual(r.params, [42]);
});

test('全空 filter 返回空 where', () => {
    const r = buildBillWhere({});
    assert.equal(r.where, '');
    assert.deepEqual(r.params, []);
});

test('keyword 用 %...% 包裹', () => {
    const r = buildBillWhere({ userId: 1, keyword: '咖啡' }, { alias: 'b' });
    assert.equal(r.where, 'WHERE b.user_id = ? AND b.remark LIKE ?');
    assert.deepEqual(r.params, [1, '%咖啡%']);
});

test('endDate 自动追加 23:59:59', () => {
    const r = buildBillWhere(
        { userId: 1, startDate: '2026-05-01', endDate: '2026-05-31' },
        { alias: 'b' }
    );
    assert.deepEqual(r.params, [1, '2026-05-01', '2026-05-31 23:59:59']);
});

test('categoryId 字符串会被 parseInt', () => {
    const r = buildBillWhere({ userId: 1, categoryId: '8' });
    assert.deepEqual(r.params, [1, 8]);
});

test('only 选项只启用指定 filter', () => {
    const r = buildBillWhere(
        { userId: 1, month: '2026-05', type: 'expense', keyword: 'x' },
        { alias: 'b', only: ['userId', 'month'] }
    );
    assert.equal(r.where, 'WHERE b.user_id = ? AND DATE_FORMAT(b.bill_time, "%Y-%m") = ?');
    assert.deepEqual(r.params, [1, '2026-05']);
});

test('appendCondition 在非空 where 上追加用 AND', () => {
    const base = buildBillWhere({ userId: 1 });
    const r = appendCondition(base, "type = ?", ['EXPENSE']);
    assert.equal(r.where, 'WHERE user_id = ? AND type = ?');
    assert.deepEqual(r.params, [1, 'EXPENSE']);
});

test('appendCondition 在空 where 上变成新 WHERE', () => {
    const r = appendCondition({ where: '', params: [] }, 'type = ?', ['INCOME']);
    assert.equal(r.where, 'WHERE type = ?');
    assert.deepEqual(r.params, ['INCOME']);
});

test('unknown filter key 抛错', () => {
    assert.throws(
        () => buildBillWhere({ userId: 1 }, { only: ['bogus'] }),
        /unknown filter "bogus"/
    );
});

test('回归：旧实现 .replace("user_id = ?", ...) 在多次出现时只会替换第一个；新实现按字段构造不存在此问题', () => {
    // 把 alias=b 的结果再叠加一个手写的 user_id 条件，模拟以前 .replace 会失手的场景
    const base = buildBillWhere({ userId: 1 }, { alias: 'b' });
    const r = appendCondition(base, 'b.user_id = ?', [1]); // 同字段、同字面量
    // 两个 user_id 条件、两个 ? 参数都保留了，参数顺序与 ? 一致
    assert.equal(r.where, 'WHERE b.user_id = ? AND b.user_id = ?');
    assert.deepEqual(r.params, [1, 1]);
});

console.log(`\n${passed} passed`);
