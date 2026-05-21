#!/usr/bin/env bash
set -u

BASE="http://localhost:3000"
PASS=0
FAIL=0
TOKEN=""
PHONE="13900001111"
ORIG_PASS="13900001111"

check() {
  local id="$1" desc="$2" expected="$3" actual_code="$4"
  if [ "$expected" = "$actual_code" ]; then
    echo "[PASS] $id: $desc - response: {code: $actual_code}"
    PASS=$((PASS+1))
  else
    echo "[FAIL] $id: $desc - response: {code: $actual_code}"
    FAIL=$((FAIL+1))
  fi
}

check_gte() {
  local id="$1" desc="$2" min="$3" actual_code="$4"
  if [ "$actual_code" -ge "$min" ] 2>/dev/null; then
    echo "[PASS] $id: $desc - response: {code: $actual_code}"
    PASS=$((PASS+1))
  else
    echo "[FAIL] $id: $desc - response: {code: $actual_code}"
    FAIL=$((FAIL+1))
  fi
}

check_not() {
  local id="$1" desc="$2" not_expected="$3" actual_code="$4"
  if [ "$actual_code" != "$not_expected" ]; then
    echo "[PASS] $id: $desc - response: {code: $actual_code}"
    PASS=$((PASS+1))
  else
    echo "[FAIL] $id: $desc - response: {code: $actual_code}"
    FAIL=$((FAIL+1))
  fi
}

extract_code() {
  echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null || echo ""
}

extract_token() {
  echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null || echo ""
}

echo "========================================="
echo "  Authentication Module Test Suite"
echo "========================================="
echo ""

# ============================================================
# AUTH-04: 正常登录 — need token for auth-required tests
# ============================================================
echo "--- LOGIN PHASE ---"
LOGIN_RESP=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"$ORIG_PASS\"}")
LOGIN_CODE=$(extract_code "$LOGIN_RESP")
TOKEN=$(extract_token "$LOGIN_RESP")
check "AUTH-04" "正常登录" "0" "$LOGIN_CODE"
echo "  Token: ${TOKEN:0:20}..."
echo ""

# ============================================================
# AUTH-01: 重复注册
# ============================================================
echo "--- AUTH-01: 重复注册 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"$ORIG_PASS\"}")
C=$(extract_code "$R")
check "AUTH-01" "重复注册" "409" "$C"
echo ""

# ============================================================
# AUTH-03: 空密码注册
# ============================================================
echo "--- AUTH-03: 空密码注册 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"\"}")
C=$(extract_code "$R")
check "AUTH-03" "空密码注册" "400" "$C"
echo ""

# ============================================================
# AUTH-05: 密码错误
# ============================================================
echo "--- AUTH-05: 密码错误 ---"
R=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"WrongPass999\"}")
C=$(extract_code "$R")
check "AUTH-05" "密码错误" "401" "$C"
echo ""

# ============================================================
# AUTH-06: 未注册手机号登录
# ============================================================
echo "--- AUTH-06: 未注册手机号登录 ---"
R=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800009999\",\"password\":\"somepass\"}")
C=$(extract_code "$R")
check_gte "AUTH-06" "未注册手机号登录" "400" "$C"
echo ""

# ============================================================
# AUTH-07: 空密码登录
# ============================================================
echo "--- AUTH-07: 空密码登录 ---"
R=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"\"}")
C=$(extract_code "$R")
check "AUTH-07" "空密码登录" "400" "$C"
echo ""

# ============================================================
# AUTH-08: 修改密码成功
# ============================================================
echo "--- AUTH-08: 修改密码成功 ---"
R=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"$ORIG_PASS\",\"new_password\":\"NewPass123\"}")
C=$(extract_code "$R")
check "AUTH-08.1" "修改密码(改)" "0" "$C"

# login with new password
R=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"NewPass123\"}")
C=$(extract_code "$R")
check "AUTH-08.2" "新密码登录" "0" "$C"

# restore password
R=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"NewPass123\",\"new_password\":\"$ORIG_PASS\"}")
C=$(extract_code "$R")
check "AUTH-08.3" "恢复密码" "0" "$C"

# verify restored
R=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"$ORIG_PASS\"}")
C=$(extract_code "$R")
check "AUTH-08.4" "验证恢复" "0" "$C"
echo ""

# ============================================================
# AUTH-09: 旧密码错误
# ============================================================
echo "--- AUTH-09: 旧密码错误 ---"
R=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"WrongOldPass\",\"new_password\":\"AnotherPass456\"}")
C=$(extract_code "$R")
check "AUTH-09" "旧密码错误" "401" "$C"
echo ""

# ============================================================
# AUTH-BN-01: 手机号含字母
# ============================================================
echo "--- AUTH-BN-01: 手机号含字母 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"abc123\",\"password\":\"somepass123\"}")
C=$(extract_code "$R")
check "AUTH-BN-01" "手机号含字母" "400" "$C"
echo ""

# ============================================================
# AUTH-BN-02: 手机号过短
# ============================================================
echo "--- AUTH-BN-02: 手机号过短 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"1234567\",\"password\":\"somepass123\"}")
C=$(extract_code "$R")
check "AUTH-BN-02" "手机号过短" "400" "$C"
echo ""

# ============================================================
# AUTH-BN-03: 手机号过长
# ============================================================
echo "--- AUTH-BN-03: 手机号过长 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"1234567890123456\",\"password\":\"somepass123\"}")
C=$(extract_code "$R")
check "AUTH-BN-03" "手机号过长" "400" "$C"
echo ""

# ============================================================
# AUTH-BN-04: 空手机号注册
# ============================================================
echo "--- AUTH-BN-04: 空手机号注册 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"\",\"password\":\"somepass123\"}")
C=$(extract_code "$R")
check "AUTH-BN-04" "空手机号注册" "400" "$C"
echo ""

# ============================================================
# AUTH-BN-07: 密码长度不足
# ============================================================
echo "--- AUTH-BN-07: 密码长度不足 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13899998888\",\"password\":\"123\"}")
C=$(extract_code "$R")
check "AUTH-BN-07" "密码长度不足" "400" "$C"
echo ""

# ============================================================
# AUTH-BN-08: 修改密码-新密码过短
# ============================================================
echo "--- AUTH-BN-08: 修改密码-新密码过短 ---"
R=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"$ORIG_PASS\",\"new_password\":\"12345\"}")
C=$(extract_code "$R")
check "AUTH-BN-08" "修改密码-新密码过短" "400" "$C"
echo ""

# ============================================================
# AUTH-WB-01: Phone format validation - phone with space
# ============================================================
echo "--- AUTH-WB-01: 手机号含空格 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"12 345\",\"password\":\"somepass123\"}")
C=$(extract_code "$R")
check "AUTH-WB-01" "手机号含空格" "400" "$C"
echo ""

# ============================================================
# AUTH-WB-02: phone="12345678" — expect to pass format check (0 or 409 but not 400 "格式不正确")
# ============================================================
echo "--- AUTH-WB-02: 8位纯数字手机号 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"12345678\",\"password\":\"somepass123\"}")
C=$(extract_code "$R")
check_not "AUTH-WB-02" "8位纯数字手机号不报格式错" "400" "$C"
echo ""

# ============================================================
# AUTH-WB-03: phone="123456789012345" — expect to pass format check
# ============================================================
echo "--- AUTH-WB-03: 15位纯数字手机号 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"123456789012345\",\"password\":\"somepass123\"}")
C=$(extract_code "$R")
check_not "AUTH-WB-03" "15位纯数字手机号不报格式错" "400" "$C"
echo ""

# ============================================================
# AUTH-WB-04: 用户已存在
# ============================================================
echo "--- AUTH-WB-04: 用户已存在 ---"
R=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"$ORIG_PASS\"}")
C=$(extract_code "$R")
check "AUTH-WB-04" "用户已存在" "409" "$C"
echo ""

# ============================================================
# AUTH-WB-05: Default nickname check
# ============================================================
echo "--- AUTH-WB-05: Default nickname check ---"
R=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"$ORIG_PASS\"}")
C=$(extract_code "$R")
NICKNAME=$(echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('user',{}).get('nickname','NOT_FOUND'))" 2>/dev/null || echo "ERROR")
if [ "$C" = "0" ] && [ "$NICKNAME" != "NOT_FOUND" ] && [ "$NICKNAME" != "ERROR" ]; then
  echo "[PASS] AUTH-WB-05: Default nickname check - response: {code: $C, nickname: $NICKNAME}"
  PASS=$((PASS+1))
else
  echo "[FAIL] AUTH-WB-05: Default nickname check - response: {code: $C, nickname: $NICKNAME}"
  FAIL=$((FAIL+1))
fi
echo ""

# ============================================================
# AUTH-WB-09: Password length validation - 6 chars should pass
# ============================================================
echo "--- AUTH-WB-09: 新密码6位应通过 ---"
R=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"$ORIG_PASS\",\"new_password\":\"123456\"}")
C=$(extract_code "$R")
check "AUTH-WB-09" "新密码6位应通过" "0" "$C"

# restore after WB-09
R2=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"123456\",\"new_password\":\"$ORIG_PASS\"}")
C2=$(extract_code "$R2")
if [ "$C2" != "0" ]; then
  echo "  [WARN] Restore after WB-09 returned code=$C2"
fi
echo ""

# ============================================================
# AUTH-WB-10: Password length validation - 5 chars should fail
# ============================================================
echo "--- AUTH-WB-10: 新密码5位应失败 ---"
R=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"$ORIG_PASS\",\"new_password\":\"12345\"}")
C=$(extract_code "$R")
check "AUTH-WB-10" "新密码5位应失败" "400" "$C"
echo ""

# ============================================================
# AUTH-WB-11: Old password wrong on password change
# ============================================================
echo "--- AUTH-WB-11: 修改密码-旧密码错误 ---"
R=$(curl -s -X PUT "$BASE/api/user/password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"TotallyWrong000\",\"new_password\":\"SomeNewPass123\"}")
C=$(extract_code "$R")
check "AUTH-WB-11" "修改密码-旧密码错误" "401" "$C"
echo ""

# ============================================================
# AUTH-WB-12: 无 Authorization header
# ============================================================
echo "--- AUTH-WB-12: 无 Authorization header ---"
R=$(curl -s -X GET "$BASE/api/user/profile")
C=$(extract_code "$R")
check "AUTH-WB-12" "无Authorization header" "401" "$C"
echo ""

# ============================================================
# AUTH-WB-13: Bearer前缀缺失
# ============================================================
echo "--- AUTH-WB-13: Bearer前缀缺失 ---"
R=$(curl -s -X GET "$BASE/api/user/profile" \
  -H "Authorization: $TOKEN")
C=$(extract_code "$R")
check "AUTH-WB-13" "Bearer前缀缺失" "401" "$C"
echo ""

# ============================================================
# AUTH-WB-14: 过期/fake token
# ============================================================
echo "--- AUTH-WB-14: 过期/伪造token ---"
R=$(curl -s -X GET "$BASE/api/user/profile" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwaG9uZSI6IjEzOTAwMDAxMTExIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9.faketoken1234567890fake")
C=$(extract_code "$R")
check "AUTH-WB-14" "伪造token" "401" "$C"
echo ""

# ============================================================
# AUTH-WB-15: 错误签名 token (malformed)
# ============================================================
echo "--- AUTH-WB-15: 错误签名token ---"
R=$(curl -s -X GET "$BASE/api/user/profile" \
  -H "Authorization: Bearer notavalidtoken.at.all")
C=$(extract_code "$R")
check "AUTH-WB-15" "错误签名token" "401" "$C"
echo ""

# ============================================================
# FINAL PASSWORD RESTORATION CHECK
# ============================================================
echo "--- FINAL CHECK: Restore password to $ORIG_PASS ---"
RESTORE_R=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"password\":\"$ORIG_PASS\"}")
RESTORE_C=$(extract_code "$RESTORE_R")
if [ "$RESTORE_C" = "0" ]; then
  echo "[OK] Password is correctly set to original: $ORIG_PASS"
else
  echo "[WARN] Final password check returned code=$RESTORE_C — attempting restore..."
  # try restoring one more time if we still have a valid token
  curl -s -X PUT "$BASE/api/user/password" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"old_password\":\"NewPass123\",\"new_password\":\"$ORIG_PASS\"}" > /dev/null
  curl -s -X PUT "$BASE/api/user/password" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"old_password\":\"123456\",\"new_password\":\"$ORIG_PASS\"}" > /dev/null
fi
echo ""

# ============================================================
# SUMMARY
# ============================================================
TOTAL=$((PASS+FAIL))
echo "========================================="
echo "  Auth Module Summary: $PASS pass, $FAIL fail (total: $TOTAL)"
echo "========================================="
