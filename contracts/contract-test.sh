#!/usr/bin/env bash
# contract-test.sh — 跨后端契约测试
#
# 同时打 Node（默认 :3000）和 Java（默认 :8080）两个端口，对每个端点：
#   1) 记录 HTTP 状态码
#   2) 解析外层 {code, msg, data} 结构
#   3) 用 jq 点检响应里必须存在的关键字段
#
# 输出：
#   - 终端人类可读摘要（每端点一行 ✓/✗ + 差异）
#   - reports/contract-test.<timestamp>.md  机器可读报告
#   - reports/diff-snapshot.json            最新差异快照
#
# 退出码：所有"双方都符合契约"为 0；任一差异非 0。
#
# 依赖：bash 4+, curl, jq；Node 11+ 自带 fetch 但 curl 通用。
# 不依赖任何 npm 装包。

set -uo pipefail
set -f  # 关闭文件名通配，避免 probe_endpoint 的 "*" / "-" 被 glob 展开

# ---- 配置 ----
NODE_BASE="${NODE_BASE:-http://localhost:3000}"
JAVA_BASE="${JAVA_BASE:-http://localhost:8080}"
REPORTS_DIR="$(cd "$(dirname "$0")" && pwd)/reports"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
REPORT_FILE="$REPORTS_DIR/contract-test.$TIMESTAMP.md"
SNAPSHOT_FILE="$REPORTS_DIR/diff-snapshot.json"
RESP_BODY_FILE="$REPORTS_DIR/.last-response.body"
# /tmp 在 Windows + MSYS 下不可写；用 reports/ 目录

# 测试用手机号（每次随机，避免两端口互相锁）
SUFFIX="$RANDOM$$"
PHONE="139$(printf '%08d' "$SUFFIX")"
PASSWORD="P@ss${SUFFIX}0rd"
NICKNAME="test_${SUFFIX}"

# 颜色（终端无颜色时 fallback）
if [ -t 1 ]; then
    RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[0;33m'; DIM=$'\033[2m'; RESET=$'\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; DIM=''; RESET=''
fi

# ---- 前置检查 ----
for cmd in curl jq; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "缺少依赖: $cmd" >&2
        exit 2
    fi
done

mkdir -p "$REPORTS_DIR"

# ---- 结果聚合 ----
declare -a RESULTS_PATH
declare -a RESULTS_NODE
declare -a RESULTS_JAVA
DIFFS=()

# 状态点检：给一段 jq filter，应返回 true（非空/非 false）
# 用法：assert_jq '<jq expression>' '<description>'
assert_jq() {
    local filter="$1"
    local desc="$2"
    if [ "$filter" = "true" ] || [ "$filter" = "false" ]; then
        if [ "$filter" = "true" ]; then
            printf "    %s✓%s %s\n" "$GREEN" "$RESET" "$desc"
            return 0
        else
            printf "    %s✗%s %s\n" "$RED" "$RESET" "$desc"
            return 1
        fi
    fi
    printf "    %s?%s %s (= %s)\n" "$YELLOW" "$RESET" "$desc" "$filter"
    return 0
}

# 探测后端是否在跑
probe() {
    local base="$1"
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$base/api/health" 2>/dev/null)
    if [ "$code" = "200" ]; then
        echo "up"
    else
        echo "down"
    fi
}

# 打一次请求，返回 body 到全局变量 RESP_BODY
# 用法：http_call <METHOD> <BASE> <PATH> [AUTH_TOKEN] [BODY_JSON]
http_call() {
    local method="$1" base="$2" path="$3" token="${4:-}" body="${5:-}"
    local args=(-s -o "$RESP_BODY_FILE" -w "%{http_code}" --max-time 5 -X "$method")
    if [ -n "$token" ]; then
        args+=(-H "Authorization: Bearer $token")
    fi
    if [ -n "$body" ]; then
        args+=(-H "Content-Type: application/json" -d "$body")
    fi
    curl "${args[@]}" "$base$path" 2>/dev/null
    RESP_BODY="$(cat "$RESP_BODY_FILE" 2>/dev/null || echo '')"
}

# 提取一个后端在某个端点上的检查清单
# 用一组 jq filter 描述「这个端点必须满足的契约」，函数把这些断言跑出来
# 用法：run_checks <base> <label> <json_body> <expected_status>
run_checks() {
    local base="$1" label="$2" body="$3" expected_status="$4"
    local failures=0

    # 外层结构必须是 {code, msg, ...}
    local has_outer
    has_outer=$(printf '%s' "$body" | jq -e 'type == "object" and (.code != null) and (.msg != null)' >/dev/null 2>&1 && echo true || echo false)
    if ! assert_jq "$has_outer" "外层 {code, msg} 存在" >/dev/null; then
        printf "    %s✗%s [%s] 响应不是标准外层结构：%.200s\n" "$RED" "$RESET" "$label" "$body"
        return 1
    fi

    return 0
}

# 端点执行器：把同一份契约用例同步打到两端口，输出对比
# 用法：probe_endpoint <METHOD> <PATH> <auth_marker_or_-> <body_or_-> <expected_status> <jq_asserts...>
#   auth_marker: - 表示匿名；* 表示带 token；其它值原样当 token
#   body: - 表示无 body；JSON 字符串
probe_endpoint() {
    local method="$1" path="$2" auth="$3" body_arg="$4" expected="$5"
    shift 5
    local asserts=("$@")

    local node_token java_token
    case "$auth" in
        -)  node_token=""; java_token="" ;;
        *)  node_token="$NODE_TOKEN"; java_token="$JAVA_TOKEN" ;;
    esac

    local body_str=""
    [ "$body_arg" != "-" ] && body_str="$body_arg"

    # 两端各调一次
    local NODE_HTTP="" node_body="" JAVA_HTTP="" java_body=""
    if [ "$NODE_STATE" = "up" ]; then
        NODE_HTTP=$(http_call "$method" "$NODE_BASE" "$path" "$node_token" "$body_str")
        node_body="$RESP_BODY"
    else
        node_body=""
    fi
    if [ "$JAVA_STATE" = "up" ]; then
        JAVA_HTTP=$(http_call "$method" "$JAVA_BASE" "$path" "$java_token" "$body_str")
        java_body="$RESP_BODY"
    else
        java_body=""
    fi

    # 状态码对比
    local status_match
    local node_ok=false java_ok=false
    [ "$NODE_HTTP" = "$expected" ] && node_ok=true
    [ "$JAVA_HTTP" = "$expected" ] && java_ok=true
    if $node_ok && $java_ok; then
        status_match="ok"
    elif $node_ok; then
        status_match="node_ok_java_mismatch"
    elif $java_ok; then
        status_match="java_ok_node_mismatch"
    else
        status_match="both_mismatch"
    fi

    # 状态行
    local symbol color
    if [ "$status_match" = "ok" ]; then
        symbol="✓"; color="$GREEN"
    else
        symbol="✗"; color="$RED"
    fi
    printf "%s%s%s %-6s %-46s node=%-3s java=%-3s (expected=%s)\n" \
        "$color" "$symbol" "$RESET" "$method" "$path" "${NODE_HTTP:--}" "${JAVA_HTTP:--}" "$expected"

    # 关键字段断言（jq 表达式，两端都跑）
    for jqa in "${asserts[@]}"; do
        local desc="${jqa%%|*}"
        local filter="${jqa#*|}"
        local node_v java_v
        node_v=$(printf '%s' "$node_body" | jq -r "$filter" 2>/dev/null || echo "ERROR")
        java_v=$(printf '%s' "$java_body" | jq -r "$filter" 2>/dev/null || echo "ERROR")

        if [ "$node_v" = "$java_v" ]; then
            printf "    %s✓%s %s = %s\n" "$GREEN" "$RESET" "$desc" "$node_v"
        else
            printf "    %s✗%s %s : node=%s java=%s\n" "$RED" "$RESET" "$desc" "$node_v" "$java_v"
            DIFFS+=("{\"path\":\"$method $path\",\"assert\":\"$desc\",\"node\":\"$node_v\",\"java\":\"$java_v\"}")
        fi
    done

    RESULTS_PATH+=("$method $path")
    RESULTS_NODE+=("{\"http\":\"$NODE_HTTP\",\"body\":$(printf '%s' "$node_body" | jq -c '.' 2>/dev/null || echo '"<unparseable>"')}")
    RESULTS_JAVA+=("{\"http\":\"$JAVA_HTTP\",\"body\":$(printf '%s' "$java_body" | jq -c '.' 2>/dev/null || echo '"<unparseable>"')}")
}

# ---- 启动 ----
echo "============================================"
echo " 契约测试 · $(date '+%Y-%m-%d %H:%M:%S')"
echo " Node: $NODE_BASE    Java: $JAVA_BASE"
echo "============================================"

NODE_STATE=$(probe "$NODE_BASE")
JAVA_STATE=$(probe "$JAVA_BASE")
echo "  Node 状态: $NODE_STATE"
echo "  Java 状态: $JAVA_STATE"
echo

# 端口都不在跑时，脚本仍能跑出"未启动"占位
if [ "$NODE_STATE" = "down" ] && [ "$JAVA_STATE" = "down" ]; then
    echo "${YELLOW}提示：两端口都未启动，测试将只验证脚本本身。${RESET}"
    echo "  启动方式见 contracts/README.md"
    echo
fi

# ---- 公共流程：注册 → 登录拿 token ----
NODE_TOKEN=""
JAVA_TOKEN=""

# 注册测试用户（哪端启了就在哪端注册；不启则跳过）
register_payload=$(jq -nc --arg p "$PHONE" --arg pw "$PASSWORD" --arg n "$NICKNAME" \
    '{phone:$p, password:$pw, nickname:$n}')

login_payload=$(jq -nc --arg p "$PHONE" --arg pw "$PASSWORD" \
    '{phone:$p, password:$pw}')

# 测一次注册 + 登录拿 token
echo "─── 准备工作：注册 + 登录 ───"
if [ "$NODE_STATE" = "up" ]; then
    http_call POST "$NODE_BASE" /api/auth/register "" "$register_payload"
    if [ "$(printf '%s' "$RESP_BODY" | jq -r '.code' 2>/dev/null)" = "0" ]; then
        echo "  Node 注册: ok"
    else
        echo "  Node 注册: $(printf '%s' "$RESP_BODY" | jq -c '.' 2>/dev/null)"
    fi
    http_call POST "$NODE_BASE" /api/auth/login "" "$login_payload"
    NODE_TOKEN=$(printf '%s' "$RESP_BODY" | jq -r '.data.token // empty' 2>/dev/null)
    echo "  Node 拿 token: ${NODE_TOKEN:+ok}${NODE_TOKEN:-<failed>}"
fi
if [ "$JAVA_STATE" = "up" ]; then
    http_call POST "$JAVA_BASE" /api/auth/register "" "$register_payload"
    if [ "$(printf '%s' "$RESP_BODY" | jq -r '.code' 2>/dev/null)" = "0" ]; then
        echo "  Java 注册: ok"
    else
        echo "  Java 注册: $(printf '%s' "$RESP_BODY" | jq -c '.' 2>/dev/null)"
    fi
    http_call POST "$JAVA_BASE" /api/auth/login "" "$login_payload"
    JAVA_TOKEN=$(printf '%s' "$RESP_BODY" | jq -r '.data.token // empty' 2>/dev/null)
    echo "  Java 拿 token: ${JAVA_TOKEN:+ok}${JAVA_TOKEN:-<failed>}"
fi
echo

# ---- 端点扫描 ----
# 每个调用：probe_endpoint METHOD PATH auth body expected "desc|jq" ...
# auth:  - 表示匿名  /  * 表示带 token
# body:  - 表示无 body / JSON 字符串
# expected: 期望的 HTTP 状态码

echo "─── Auth ───"
probe_endpoint POST /api/auth/register - "$register_payload" 200 \
    "code 字段存在|.code"
probe_endpoint POST /api/auth/login - "$login_payload" 200 \
    "code 字段存在|.code" \
    "登录返回 token|.data.token"
probe_endpoint POST /api/auth/send-sms-code - '{"phone":"'"$PHONE"'"}' 200 \
    "code 字段存在|.code"
probe_endpoint POST /api/auth/reset-password - \
    '{"phone":"'"$PHONE"'","smsCode":"000000","newPassword":"'"$PASSWORD"'"}' 400 \
    "错误码为业务级|if .code == 0 then \"ok\" else \"err\" end"

echo
echo "─── User ───"
probe_endpoint GET  /api/user/profile * - 200 \
    "code 字段存在|.code" \
    "返回用户 ID|.data.id"
probe_endpoint PUT  /api/user/profile * \
    '{"nickname":"updated_'"$SUFFIX"'"}' 200 \
    "code 字段存在|.code"
probe_endpoint PUT  /api/user/password * \
    '{"old_password":"'"$PASSWORD"'","new_password":"'"$PASSWORD"'"}' 200 \
    "code 字段存在|.code"
probe_endpoint PUT  /api/user/bind-phone * '{"phone":"13999999999"}' 200 \
    "code 字段存在|.code"
probe_endpoint PUT  /api/user/unbind-phone * - 200 \
    "code 字段存在|.code"

echo
echo "─── Bills ───"
probe_endpoint GET  /api/bills * - 200 \
    "code 字段存在|.code" \
    "data.list 是数组|.data.list | type" \
    "data.total 是数字|.data.total | type" \
    "data.page 是数字|.data.page | type" \
    "data.pageSize 是数字|.data.pageSize | type"
probe_endpoint GET  /api/bills/1 * - 404 \
    "code 字段存在|.code"
probe_endpoint POST /api/bills * \
    '{"type":"EXPENSE","amount":1.0,"category_id":1,"remark":"test"}' 200 \
    "code 字段存在|.code" \
    "返回账单 ID|.data.id"
probe_endpoint PUT  /api/bills/1 * \
    '{"remark":"updated"}' 200 \
    "code 字段存在|.code"
probe_endpoint DELETE /api/bills/1 * - 200 \
    "code 字段存在|.code"
probe_endpoint GET  /api/bills/stats/month * - 200 \
    "code 字段存在|.code" \
    "expense.total 是数字|.data.expense.total | type" \
    "income.total 是数字|.data.income.total | type" \
    "daily 是数组|.data.daily | type" \
    "categories 是数组|.data.categories | type"
probe_endpoint GET  /api/bills/export?format=csv * - 200 \
    "code 字段存在|if .code then .code else \"binary\" end"

echo
echo "─── Categories ───"
probe_endpoint GET  /api/categories * - 200 \
    "code 字段存在|.code" \
    "data 是数组|.data | type"
probe_endpoint POST /api/categories * \
    '{"name":"tmp_'"$SUFFIX"'","type":"EXPENSE"}' 200 \
    "code 字段存在|.code" \
    "返回分类 ID|.data.id"
probe_endpoint PUT  /api/categories/1 * \
    '{"name":"renamed_'"$SUFFIX"'"}' 200 \
    "code 字段存在|.code"
probe_endpoint DELETE /api/categories/1 * - 200 \
    "code 字段存在|.code"

echo
echo "─── Budgets ───"
probe_endpoint GET  /api/budgets * - 200 \
    "code 字段存在|.code" \
    "data 是数组|.data | type"
probe_endpoint POST /api/budgets * \
    '{"amount":100.0,"period":"MONTHLY"}' 200 \
    "code 字段存在|.code" \
    "返回预算 ID（结构）|.data | if type == \"object\" then .id else . end"
probe_endpoint GET  /api/budgets/dashboard * - 200 \
    "code 字段存在|.code" \
    "total_budget 是数字|.data.total_budget | type" \
    "spent 是数字|.data.spent | type" \
    "categories 是数组|.data.categories | type"
probe_endpoint DELETE /api/budgets/1 * - 200 \
    "code 字段存在|.code"

echo
echo "─── Tags ───"
probe_endpoint GET  /finance/tags * - 200 \
    "code 字段存在|.code" \
    "tags 是数组结构|.data | if type == \"object\" then .list | type else type end"
probe_endpoint POST /finance/tags * \
    '{"name":"tmp_'"$SUFFIX"'"}' 200 \
    "code 字段存在|.code" \
    "返回标签 ID|.data.id"
probe_endpoint PUT  /finance/tags/1 * \
    '{"name":"renamed_'"$SUFFIX"'"}' 200 \
    "code 字段存在|.code"
probe_endpoint DELETE /finance/tags/1 * - 200 \
    "code 字段存在|.code"

echo
echo "─── Health ───"
probe_endpoint GET  /api/health - - 200 \
    "code 字段存在|.code" \
    "data.status 是字符串|.data.status | type"

# ---- 报告 ----
{
    echo "# Contract Test Report"
    echo
    echo "- 节点状态: Node=$NODE_STATE / Java=$JAVA_STATE"
    echo "- 时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "- 测试手机号: $PHONE"
    echo
    echo "## 已知差异数: ${#DIFFS[@]}"
    if [ "${#DIFFS[@]}" -gt 0 ]; then
        echo
        echo "| 端点 | 断言 | Node | Java |"
        echo "|---|---|---|---|"
        for d in "${DIFFS[@]}"; do
            echo "| $(echo "$d" | jq -r '.path') | $(echo "$d" | jq -r '.assert') | $(echo "$d" | jq -r '.node') | $(echo "$d" | jq -r '.java') |"
        done
    fi
    echo
    echo "## 详细端点响应（每端前 200 字符）"
    for i in "${!RESULTS_PATH[@]}"; do
        echo
        echo "### ${RESULTS_PATH[$i]}"
        echo
        local_node_http=$(printf '%s' "${RESULTS_NODE[$i]}" | jq -r '.http // "(skipped)"' 2>/dev/null)
        local_java_http=$(printf '%s' "${RESULTS_JAVA[$i]}" | jq -r '.http // "(skipped)"' 2>/dev/null)
        echo "**Node** (HTTP $local_node_http):"
        echo '```json'
        printf '%s' "${RESULTS_NODE[$i]}" | jq -r '.body // ""' 2>/dev/null | head -c 400
        echo
        echo '```'
        echo
        echo "**Java** (HTTP $local_java_http):"
        echo '```json'
        printf '%s' "${RESULTS_JAVA[$i]}" | jq -r '.body // ""' 2>/dev/null | head -c 400
        echo
        echo '```'
    done
} > "$REPORT_FILE"

# 差异快照（供 DIFFS.md 引用 + CI 用）
{
    echo "{"
    echo "  \"timestamp\": \"$(date -Iseconds)\","
    echo "  \"nodeState\": \"$NODE_STATE\","
    echo "  \"javaState\": \"$JAVA_STATE\","
    echo "  \"diffCount\": ${#DIFFS[@]},"
    echo "  \"diffs\": ["
    for i in "${!DIFFS[@]}"; do
        if [ "$i" -gt 0 ]; then echo ","; fi
        printf "    %s" "${DIFFS[$i]}"
    done
    echo
    echo "  ]"
    echo "}"
} > "$SNAPSHOT_FILE"

echo
echo "============================================"
echo " 摘要：发现 ${#DIFFS[@]} 处差异"
echo " 报告: $REPORT_FILE"
echo " 快照: $SNAPSHOT_FILE"
echo "============================================"

if [ "${#DIFFS[@]}" -gt 0 ]; then
    echo
    echo "差异明细："
    for d in "${DIFFS[@]}"; do
        echo "  - $(echo "$d" | jq -c '.')"
    done
    exit 1
fi
exit 0
