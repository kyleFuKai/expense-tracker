/**
 * Bill query builder
 *
 * 统一构造账单查询的 WHERE 子句与参数数组，避免在 routes 里用字符串 .replace
 * 拼 SQL（旧实现 bills.js 里的 user_id = ? → b.user_id = ? 这种替换非常脆弱：
 * 只替换第一个匹配、依赖 conditions 顺序、params.slice(1) 隐式约定）。
 *
 * 设计原则：
 *   1. 每个 filter 自己声明 SQL 片段（带别名占位 `{a}.`）和参数；
 *   2. 调用时用具体 alias 替换占位，避免运行时字符串编辑；
 *   3. SQL 片段是模块内常量，不接收用户输入，无注入面。
 */

// Filter 定义。sql 中 `{a}.` 是别名占位符，buildBillWhere 时根据 alias 替换为
// 实际前缀（如 'b.' 或空串）。每个 filter 决定自己是否参与（when）和参数值（value）。
const FILTERS = {
    userId: {
        sql: '{a}.user_id = ?',
        when: (f) => f.userId !== undefined && f.userId !== null,
        value: (f) => [f.userId],
    },
    month: {
        sql: 'DATE_FORMAT({a}.bill_time, "%Y-%m") = ?',
        when: (f) => !!f.month,
        value: (f) => [f.month],
    },
    type: {
        sql: '{a}.type = ?',
        when: (f) => !!f.type,
        value: (f) => [String(f.type).toUpperCase()],
    },
    categoryId: {
        sql: '{a}.category_id = ?',
        when: (f) => f.categoryId !== undefined && f.categoryId !== null && f.categoryId !== '',
        value: (f) => [parseInt(f.categoryId)],
    },
    keyword: {
        sql: '{a}.remark LIKE ?',
        when: (f) => !!f.keyword,
        value: (f) => [`%${f.keyword}%`],
    },
    startDate: {
        sql: '{a}.bill_time >= ?',
        when: (f) => !!f.startDate,
        value: (f) => [f.startDate],
    },
    endDate: {
        sql: '{a}.bill_time <= ?',
        when: (f) => !!f.endDate,
        value: (f) => [f.endDate + ' 23:59:59'],
    },
    // tagId 不是 WHERE 条件而是 JOIN 上的过滤，单独由 buildBillQuery 处理
};

/**
 * 用别名替换 SQL 片段中的 `{a}.` 占位。alias 为空串时直接去掉前缀。
 */
function applyAlias(sql, alias) {
    const prefix = alias ? alias + '.' : '';
    return sql.replace(/\{a\}\./g, prefix);
}

/**
 * 构造 WHERE 子句和参数。
 *
 * @param {Object} filters - { userId, month, type, categoryId, keyword, startDate, endDate }
 * @param {Object} [opts]
 * @param {string} [opts.alias=''] - 列前缀，例如 'b' 用于 `FROM bill b`；空串表示不加前缀
 * @param {string[]} [opts.only] - 仅启用列出的 filter 键；默认全部
 * @returns {{ where: string, params: any[] }} where 为 '' 或 'WHERE ...'
 */
function buildBillWhere(filters, opts = {}) {
    const alias = opts.alias || '';
    const keys = opts.only || Object.keys(FILTERS);

    const parts = [];
    const params = [];
    for (const key of keys) {
        const def = FILTERS[key];
        if (!def) throw new Error(`buildBillWhere: unknown filter "${key}"`);
        if (!def.when(filters)) continue;
        parts.push(applyAlias(def.sql, alias));
        params.push(...def.value(filters));
    }

    const where = parts.length ? 'WHERE ' + parts.join(' AND ') : '';
    return { where, params };
}

/**
 * 在已有 WHERE 之后追加一个固定条件，参数顺序保持一致。
 * 用于 stats 里需要在公共 where 上再叠加 type = 'EXPENSE' 等场景。
 */
function appendCondition({ where, params }, sql, extraParams = []) {
    const next = where ? `${where} AND ${sql}` : `WHERE ${sql}`;
    return { where: next, params: [...params, ...extraParams] };
}

module.exports = { buildBillWhere, appendCondition };
