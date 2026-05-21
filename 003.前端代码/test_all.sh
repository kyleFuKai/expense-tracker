#!/bin/bash
BASE="http://localhost:3000/api"
PASS=0; FAIL=0
assert() {
    if echo "$3" | grep -q "$2"; then echo "  OK: $1"; PASS=$((PASS+1))
    else echo "  FAIL: $1 (期望: $2, 实际: $3)"; FAIL=$((FAIL+1)); fi
}
echo "=== Full API Tests ==="

echo ">>> Health"
assert "health" '"status":"ok"' "$(curl -s $BASE/health)"

echo ">>> Register"
R=$(curl -s -X POST "$BASE/auth/register" -H "Content-Type: application/json" -d '{"phone":"13900001111","password":"13900001111","nickname":"TestUser"}')
if echo "$R" | grep -q '"code":0\|"code":409'; then echo "  OK"; PASS=$((PASS+1)); else echo "  FAIL: $R"; FAIL=$((FAIL+1)); fi

echo ">>> Login"
R=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"phone":"13900001111","password":"13900001111"}')
TOKEN=$(echo "$R" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
[ -n "$TOKEN" ] && echo "  OK" && PASS=$((PASS+1)) || { echo "  FAIL"; FAIL=$((FAIL+1)); TOKEN=""; }

if [ -n "$TOKEN" ]; then
echo ">>> User profile"
assert "ok" '"code":0' "$(curl -s "$BASE/user/profile" -H "Authorization: Bearer $TOKEN")"

echo ">>> Change nickname"
curl -s -X PUT "$BASE/user/profile" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"nickname":"T2"}' > /dev/null
R2=$(curl -s "$BASE/user/profile" -H "Authorization: Bearer $TOKEN")
assert "updated" '"T2"' "$R2"
curl -s -X PUT "$BASE/user/profile" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"nickname":"TestUser"}' > /dev/null

echo ">>> Invalid phone"
assert "400" '"code":400' "$(curl -s -X PUT "$BASE/user/bind-phone" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"phone":"abc"}')"

echo ">>> Categories EXPENSE+INCOME"
assert "ok" '"code":0' "$(curl -s "$BASE/categories?type=EXPENSE" -H "Authorization: Bearer $TOKEN")"
assert "ok" '"code":0' "$(curl -s "$BASE/categories?type=INCOME" -H "Authorization: Bearer $TOKEN")"

echo ">>> Create category"
R=$(curl -s -X POST "$BASE/categories" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":"AutoCat","icon":"more_horiz","type":"EXPENSE"}')
CAT_ID=$(echo "$R" | grep -o '"id":[0-9]*' | tail -1 | cut -d: -f2)
assert "ok" '"code":0' "$R"

echo ">>> Empty name"
assert "400" '"code":400' "$(curl -s -X POST "$BASE/categories" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":"","icon":"x","type":"EXPENSE"}')"

echo ">>> Delete category"
assert "ok" '"code":0' "$(curl -s -X DELETE "$BASE/categories/$CAT_ID" -H "Authorization: Bearer $TOKEN")"

FIRST_CAT_ID=$(curl -s "$BASE/categories?type=EXPENSE" -H "Authorization: Bearer $TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo ">>> Create bill"
R=$(curl -s -X POST "$BASE/bills" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"type\":\"EXPENSE\",\"amount\":100,\"category_id\":$FIRST_CAT_ID,\"remark\":\"test_bill\",\"bill_time\":\"2026-05-21 10:00:00\"}")
assert "ok" '"code":0' "$R"
BILL_ID=$(echo "$R" | grep -o '"id":[0-9]*' | tail -1 | cut -d: -f2)

echo ">>> Zero amount"
assert "400" '"code":400' "$(curl -s -X POST "$BASE/bills" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"type\":\"EXPENSE\",\"amount\":0,\"category_id\":$FIRST_CAT_ID}")"

echo ">>> No category"
assert "400" '"code":400' "$(curl -s -X POST "$BASE/bills" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"type\":\"EXPENSE\",\"amount\":10}")"

echo ">>> Bill list"
R=$(curl -s "$BASE/bills?type=EXPENSE&page=1&pageSize=10" -H "Authorization: Bearer $TOKEN")
assert "ok" '"code":0' "$R"
assert "list" '"list"' "$R"
assert "total" '"total"' "$R"

echo ">>> Bill detail"
R=$(curl -s "$BASE/bills/$BILL_ID" -H "Authorization: Bearer $TOKEN")
assert "ok" '"code":0' "$R"
assert "amount" '"100.00"' "$R"
assert "category_name" '"category_name"' "$R"

echo ">>> Update bill"
assert "ok" '"code":0' "$(curl -s -X PUT "$BASE/bills/$BILL_ID" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"type\":\"EXPENSE\",\"amount\":200,\"category_id\":$FIRST_CAT_ID,\"remark\":\"modified\",\"bill_time\":\"2026-05-21 12:00:00\"}")"
R2=$(curl -s "$BASE/bills/$BILL_ID" -H "Authorization: Bearer $TOKEN")
assert "amount updated" '"200.00"' "$R2"

echo ">>> Filter by month"
assert "ok" '"code":0' "$(curl -s "$BASE/bills?month=2026-05&page=1&pageSize=10" -H "Authorization: Bearer $TOKEN")"

echo ">>> Filter by category"
assert "ok" '"code":0' "$(curl -s "$BASE/bills?category_id=$FIRST_CAT_ID&page=1&pageSize=10" -H "Authorization: Bearer $TOKEN")"

echo ">>> Delete bill"
assert "ok" '"code":0' "$(curl -s -X DELETE "$BASE/bills/$BILL_ID" -H "Authorization: Bearer $TOKEN")"

echo ">>> Non-existent bill"
assert "code" '"code":' "$(curl -s "$BASE/bills/999999" -H "Authorization: Bearer $TOKEN")"

echo ">>> Update non-existent"
assert "code" '"code":' "$(curl -s -X PUT "$BASE/bills/999999" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"type\":\"EXPENSE\",\"amount\":1,\"category_id\":$FIRST_CAT_ID}")"

echo ">>> Monthly stats"
R=$(curl -s "$BASE/bills/stats/month?month=2026-05" -H "Authorization: Bearer $TOKEN")
assert "ok" '"code":0' "$R"
assert "expense" '"expense"' "$R"
assert "income" '"income"' "$R"
assert "categories" '"categories"' "$R"
assert "daily" '"daily"' "$R"

echo ">>> Budget dashboard"
R=$(curl -s "$BASE/budgets/dashboard?month=2026-05" -H "Authorization: Bearer $TOKEN")
assert "ok" '"code":0' "$R"
assert "total_budget" '"total_budget"' "$R"
assert "spent" '"spent"' "$R"
assert "categories" '"categories"' "$R"

echo ">>> Set total budget"
assert "ok" '"code":0' "$(curl -s -X POST "$BASE/budgets" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"amount":3000,"period":"MONTHLY"}')"

echo ">>> Set category budget"
R=$(curl -s -X POST "$BASE/budgets" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"category_id\":$FIRST_CAT_ID,\"amount\":500,\"period\":\"MONTHLY\"}")
BUDGET_ID=$(echo "$R" | grep -o '"id":[0-9]*' | tail -1 | cut -d: -f2)
assert "ok" '"code":0' "$R"

echo ">>> Dashboard with category budget"
R=$(curl -s "$BASE/budgets/dashboard?month=2026-05" -H "Authorization: Bearer $TOKEN")
assert "cat_id" '"cat_id"' "$R"
assert "budget_id" '"budget_id"' "$R"

echo ">>> Zero budget"
assert "400" '"code":400' "$(curl -s -X POST "$BASE/budgets" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"amount":0}')"

echo ">>> Budget list"
assert "ok" '"code":0' "$(curl -s "$BASE/budgets" -H "Authorization: Bearer $TOKEN")"

echo ">>> Deactivate budget"
[ -n "$BUDGET_ID" ] && assert "ok" '"code":0' "$(curl -s -X DELETE "$BASE/budgets/$BUDGET_ID" -H "Authorization: Bearer $TOKEN")" || echo "  SKIP"

echo ">>> No token"
assert "401" '"code":401' "$(curl -s "$BASE/user/profile")"

echo ">>> Bad token"
assert "401" '"code":401' "$(curl -s "$BASE/user/profile" -H "Authorization: Bearer bad_token")"

echo ">>> Duplicate register"
assert "409" '"code":409' "$(curl -s -X POST "$BASE/auth/register" -H "Content-Type: application/json" -d '{"phone":"13900001111","password":"x"}')"

echo ">>> Wrong password"
assert "401" '"code":401' "$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"phone":"13900001111","password":"wrong"}')"

echo ">>> Unregistered phone"
R=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"phone":"19999999999","password":"x"}')
if echo "$R" | grep -q '"code":4'; then echo "  OK: returns 4xx"; PASS=$((PASS+1)); else echo "  FAIL: $R"; FAIL=$((FAIL+1)); fi

echo ">>> Empty password"
assert "400" '"code":400' "$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"phone":"13900001111","password":""}')"

echo ">>> Income bill"
INC_CAT_ID=$(curl -s "$BASE/categories?type=INCOME" -H "Authorization: Bearer $TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
R=$(curl -s -X POST "$BASE/bills" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"type\":\"INCOME\",\"amount\":500,\"category_id\":$INC_CAT_ID,\"remark\":\"income_test\"}")
assert "ok" '"code":0' "$R"
INC_BILL_ID=$(echo "$R" | grep -o '"id":[0-9]*' | tail -1 | cut -d: -f2)
[ -n "$INC_BILL_ID" ] && curl -s -X DELETE "$BASE/bills/$INC_BILL_ID" -H "Authorization: Bearer $TOKEN" > /dev/null

echo ">>> Change password"
assert "ok" '"code":0' "$(curl -s -X PUT "$BASE/user/password" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"old_password":"13900001111","new_password":"NewPass1"}')"

echo ">>> New password login"
R=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"phone":"13900001111","password":"NewPass1"}')
NEW_TOKEN=$(echo "$R" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
[ -n "$NEW_TOKEN" ] && echo "  OK" && PASS=$((PASS+1)) || { echo "  FAIL"; FAIL=$((FAIL+1)); }

echo ">>> Restore password"
TOKEN="$NEW_TOKEN"
assert "ok" '"code":0' "$(curl -s -X PUT "$BASE/user/password" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"old_password":"NewPass1","new_password":"13900001111"}')"

fi

echo ""
echo "=== Results: $PASS pass, $FAIL fail ==="
