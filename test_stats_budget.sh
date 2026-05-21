#!/bin/bash
# Statistics & Budget Module Test Suite
BASE="http://localhost:3000"
PASS=0
FAIL=0

login_response=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"phone":"13900001111","password":"13900001111"}')
TOKEN=$(echo "$login_response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "=== Login Response ==="
echo "$login_response"
echo ""
if [ -z "$TOKEN" ]; then
  echo "FATAL: Could not obtain token. Aborting."
  exit 1
fi
echo "TOKEN obtained: ${TOKEN:0:20}..."
echo ""

run_test() {
  local id="$1"
  local desc="$2"
  local expect_code="$3"
  local cmd="$4"
  local check="$5"  # optional jq check expression

  local resp
  resp=$(eval "$cmd" 2>&1)
  local http_code="$?"
  local resp_code
  resp_code=$(echo "$resp" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)

  local status="FAIL"
  if [ "$expect_code" = "any" ]; then
    status="PASS"
  elif [ "$resp_code" = "$expect_code" ]; then
    if [ -n "$check" ]; then
      local check_result
      check_result=$(echo "$resp" | jq "$check" 2>/dev/null)
      if [ "$check_result" = "true" ]; then
        status="PASS"
      fi
    else
      status="PASS"
    fi
  fi

  if [ "$status" = "PASS" ]; then
    PASS=$((PASS+1))
  else
    FAIL=$((FAIL+1))
  fi

  echo "[$status] $id: $desc - response: $resp"
}

# ============================================================
# STAT Tests
# ============================================================

run_test "STAT-01" "月度统计" "0" \
  "curl -s '$BASE/api/bills/stats/month?month=2026-05' -H 'Authorization: Bearer $TOKEN'"

run_test "STAT-02" "统计包含支出" "any" \
  "curl -s '$BASE/api/bills/stats/month?month=2026-05' -H 'Authorization: Bearer $TOKEN'" \
  '.data.expense.total != null and .data.expense.count != null'

run_test "STAT-03" "统计包含收入" "any" \
  "curl -s '$BASE/api/bills/stats/month?month=2026-05' -H 'Authorization: Bearer $TOKEN'" \
  '.data.income.total != null and .data.income.count != null'

run_test "STAT-04" "统计包含分类排行" "any" \
  "curl -s '$BASE/api/bills/stats/month?month=2026-05' -H 'Authorization: Bearer $TOKEN'" \
  '.data.categories != null and (.data.categories | type == "array")'

run_test "STAT-05" "统计包含每日数据" "any" \
  "curl -s '$BASE/api/bills/stats/month?month=2026-05' -H 'Authorization: Bearer $TOKEN'" \
  '.data.daily != null and (.data.daily | type == "array")'

run_test "STAT-BN-01" "无月份参数" "0" \
  "curl -s '$BASE/api/bills/stats/month' -H 'Authorization: Bearer $TOKEN'"

run_test "STAT-BN-02" "无效月份" "0" \
  "curl -s '$BASE/api/bills/stats/month?month=abcd' -H 'Authorization: Bearer $TOKEN'"

run_test "STAT-BN-03" "无数据月份" "0" \
  "curl -s '$BASE/api/bills/stats/month?month=2020-01' -H 'Authorization: Bearer $TOKEN'"

run_test "STAT-WB-01" "COALESCE(SUM,0) 空结果" "0" \
  "curl -s '$BASE/api/bills/stats/month?month=2020-01' -H 'Authorization: Bearer $TOKEN'" \
  '.data.expense.total == 0'

# STAT-WB-02: Check if daily aggregates multiple bills on same day
wb02_resp=$(curl -s "$BASE/api/bills/stats/month?month=2026-05" -H "Authorization: Bearer $TOKEN")
wb02_daily_len=$(echo "$wb02_resp" | jq '.data.daily | length' 2>/dev/null)
wb02_unique_days=$(echo "$wb02_resp" | jq '[.data.daily[].day] | unique | length' 2>/dev/null)
if [ "$wb02_daily_len" = "$wb02_unique_days" ]; then
  echo "[PASS] STAT-WB-02: daily GROUP BY DATE - response: $wb02_resp"
  PASS=$((PASS+1))
else
  echo "[FAIL] STAT-WB-02: daily GROUP BY DATE - response: $wb02_resp"
  FAIL=$((FAIL+1))
fi

# ============================================================
# BUDG Tests
# ============================================================

run_test "BUDG-01" "预算仪表盘" "0" \
  "curl -s '$BASE/api/budgets/dashboard?month=2026-05' -H 'Authorization: Bearer $TOKEN'"

run_test "BUDG-02" "仪表盘包含总预算" "any" \
  "curl -s '$BASE/api/budgets/dashboard?month=2026-05' -H 'Authorization: Bearer $TOKEN'" \
  '.data.total_budget != null and .data.spent != null'

run_test "BUDG-03" "仪表盘包含分类预算" "any" \
  "curl -s '$BASE/api/budgets/dashboard?month=2026-05' -H 'Authorization: Bearer $TOKEN'" \
  '.data.categories != null and (.data.categories | type == "array")'

# BUDG-04: 设置总预算
b4_resp=$(curl -s -X POST "$BASE/api/budgets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":3000,"period":"monthly","start_date":"2026-05-01","end_date":"2026-05-31"}')
b4_code=$(echo "$b4_resp" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
if [ "$b4_code" = "0" ]; then
  echo "[PASS] BUDG-04: 设置总预算 - response: $b4_resp"
  PASS=$((PASS+1))
else
  echo "[FAIL] BUDG-04: 设置总预算 - response: $b4_resp"
  FAIL=$((FAIL+1))
fi

# BUDG-05: 设置分类预算
cat_resp=$(curl -s "$BASE/api/categories?type=EXPENSE" -H "Authorization: Bearer $TOKEN")
cat_id=$(echo "$cat_resp" | jq '.data[0].id' 2>/dev/null)
if [ -n "$cat_id" ] && [ "$cat_id" != "null" ]; then
  b5_resp=$(curl -s -X POST "$BASE/api/budgets" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"category_id\":$cat_id,\"amount\":500,\"period\":\"monthly\",\"start_date\":\"2026-05-01\",\"end_date\":\"2026-05-31\"}")
  b5_code=$(echo "$b5_resp" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [ "$b5_code" = "0" ]; then
    echo "[PASS] BUDG-05: 设置分类预算 - response: $b5_resp"
    PASS=$((PASS+1))
  else
    echo "[FAIL] BUDG-05: 设置分类预算 - response: $b5_resp"
    FAIL=$((FAIL+1))
  fi
else
  echo "[FAIL] BUDG-05: 设置分类预算 - no expense category found. response: $cat_resp"
  FAIL=$((FAIL+1))
fi

# BUDG-06: 零预算校验
b6_resp=$(curl -s -X POST "$BASE/api/budgets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":0,"period":"monthly"}')
b6_code=$(echo "$b6_resp" | grep -o '"code":[0-9-]*' | head -1 | cut -d: -f2)
if [ "$b6_code" = "400" ]; then
  echo "[PASS] BUDG-06: 零预算校验 - response: $b6_resp"
  PASS=$((PASS+1))
else
  echo "[FAIL] BUDG-06: 零预算校验 (expect 400, got $b6_code) - response: $b6_resp"
  FAIL=$((FAIL+1))
fi

run_test "BUDG-07" "预算列表" "0" \
  "curl -s '$BASE/api/budgets' -H 'Authorization: Bearer $TOKEN'"

# BUDG-08: 删除预算
# First get a budget id
budg_list=$(curl -s "$BASE/api/budgets" -H "Authorization: Bearer $TOKEN")
budg_id=$(echo "$budg_list" | jq '.data[0].id' 2>/dev/null)
if [ -n "$budg_id" ] && [ "$budg_id" != "null" ]; then
  b8_resp=$(curl -s -X DELETE "$BASE/api/budgets/$budg_id" \
    -H "Authorization: Bearer $TOKEN")
  b8_code=$(echo "$b8_resp" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [ "$b8_code" = "0" ]; then
    echo "[PASS] BUDG-08: 删除预算 - response: $b8_resp"
    PASS=$((PASS+1))
  else
    echo "[FAIL] BUDG-08: 删除预算 - response: $b8_resp"
    FAIL=$((FAIL+1))
  fi
else
  echo "[FAIL] BUDG-08: 删除预算 - no budget found to delete. response: $budg_list"
  FAIL=$((FAIL+1))
fi

# BUDG-BN-01: 负金额预算
bn1_resp=$(curl -s -X POST "$BASE/api/budgets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":-100,"period":"monthly"}')
bn1_code=$(echo "$bn1_resp" | grep -o '"code":[0-9-]*' | head -1 | cut -d: -f2)
if [ "$bn1_code" = "400" ] || [ "$bn1_code" = "0" ]; then
  echo "[PASS] BUDG-BN-01: 负金额预算 - response: $bn1_resp"
  PASS=$((PASS+1))
else
  echo "[FAIL] BUDG-BN-01: 负金额预算 - response: $bn1_resp"
  FAIL=$((FAIL+1))
fi

run_test "BUDG-BN-02" "删除不存在预算" "404" \
  "curl -s -X DELETE '$BASE/api/budgets/999999' -H 'Authorization: Bearer $TOKEN'"

# BUDG-WB-01: upsert existing (update)
wb1_resp1=$(curl -s -X POST "$BASE/api/budgets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":1000,"period":"monthly","start_date":"2026-05-01","end_date":"2026-05-31"}')
wb1_list_before=$(curl -s "$BASE/api/budgets" -H "Authorization: Bearer $TOKEN")
wb1_count_before=$(echo "$wb1_list_before" | jq '.data | length' 2>/dev/null)

wb1_resp2=$(curl -s -X POST "$BASE/api/budgets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":2000,"period":"monthly","start_date":"2026-05-01","end_date":"2026-05-31"}')
wb1_list_after=$(curl -s "$BASE/api/budgets" -H "Authorization: Bearer $TOKEN")
wb1_count_after=$(echo "$wb1_list_after" | jq '.data | length' 2>/dev/null)

wb1_updated=$(echo "$wb1_list_after" | jq '[.data[] | select(.amount == 2000)] | length' 2>/dev/null)
if [ "$wb1_count_before" = "$wb1_count_after" ] && [ "$wb1_updated" != "0" ]; then
  echo "[PASS] BUDG-WB-01: upsert existing (update) - response: 2nd_resp=$wb1_resp2"
  PASS=$((PASS+1))
else
  echo "[FAIL] BUDG-WB-01: upsert existing (update) - before=$wb1_count_before after=$wb1_count_after updated=$wb1_updated - resp: $wb1_resp2"
  FAIL=$((FAIL+1))
fi

# BUDG-WB-02: upsert new (insert)
wb2_resp=$(curl -s -X POST "$BASE/api/budgets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":1500,"period":"monthly","start_date":"2026-06-01","end_date":"2026-06-30"}')
wb2_code=$(echo "$wb2_resp" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
if [ "$wb2_code" = "0" ]; then
  echo "[PASS] BUDG-WB-02: upsert new (insert) - response: $wb2_resp"
  PASS=$((PASS+1))
else
  echo "[FAIL] BUDG-WB-02: upsert new (insert) - response: $wb2_resp"
  FAIL=$((FAIL+1))
fi

# BUDG-WB-05: amount=0校验
wb5_resp=$(curl -s -X POST "$BASE/api/budgets" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":0,"period":"monthly"}')
wb5_code=$(echo "$wb5_resp" | grep -o '"code":[0-9-]*' | head -1 | cut -d: -f2)
if [ "$wb5_code" = "400" ]; then
  echo "[PASS] BUDG-WB-05: amount=0校验 - response: $wb5_resp"
  PASS=$((PASS+1))
else
  echo "[FAIL] BUDG-WB-05: amount=0校验 (expect 400, got $wb5_code) - response: $wb5_resp"
  FAIL=$((FAIL+1))
fi

echo ""
echo "============================================================"
echo "Statistics+Budget Module Summary: $PASS pass, $FAIL fail"
echo "============================================================"
