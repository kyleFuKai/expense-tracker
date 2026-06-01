<template>
  <view class="login-page">
    <view class="brand-section">
      <view class="logo-icon">
        <text class="logo-text">💰</text>
      </view>
      <text class="brand-title">每日财务管家</text>
      <text class="brand-desc">简约理财，从今天开始</text>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="form-label">手机号码</text>
        <input class="form-input" v-model="phone" type="number" placeholder="请输入手机号" maxlength="11" />
      </view>

      <view class="form-item" v-if="isRegister">
        <text class="form-label">昵称</text>
        <input class="form-input" v-model="nickname" type="text" placeholder="请输入昵称（可选）" />
      </view>

      <view class="form-item">
        <text class="form-label">密码</text>
        <view class="input-with-toggle">
          <input class="form-input flex-1" v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码" maxlength="20" />
          <view class="toggle-btn" @click="showPassword = !showPassword">
            <text>{{ showPassword ? '👁️' : '👁️‍🗨️' }}</text>
          </view>
        </view>
      </view>

      <view class="form-item" v-if="isRegister">
        <text class="form-label">确认密码</text>
        <view class="input-with-toggle">
          <input class="form-input flex-1" v-model="confirmPassword" :type="showConfirm ? 'text' : 'password'" placeholder="请再次输入密码" maxlength="20" />
          <view class="toggle-btn" @click="showConfirm = !showConfirm">
            <text>{{ showConfirm ? '👁️' : '👁️‍🗨️' }}</text>
          </view>
        </view>
        <text v-if="passwordMismatch" class="error-text">两次输入的密码不一致</text>
      </view>

      <view class="forgot-link" @click="goForgot">
        <text class="forgot-text">忘记密码？</text>
      </view>

      <button class="main-btn" :disabled="loading" @click="handleSubmit">
        {{ loading ? (isRegister ? '注册中...' : '登录中...') : (isRegister ? '确认注册' : '登录') }}
      </button>
    </view>

    <view class="agreement">
      <text class="agreement-text">登录即代表您同意《用户服务协议》和《隐私保护政策》</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { request } from '../../api/request'
import { setToken } from '../../utils/storage'

const phone = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
const loading = ref(false)
const isRegister = ref(false)

const passwordMismatch = computed(() => {
  return confirmPassword.value.length > 0 && password.value !== confirmPassword.value
})

function goForgot() {
  uni.navigateTo({ url: '/pages/change-password/index' })
}

async function handleSubmit() {
  if (!phone.value) { uni.showToast({ title: '请输入手机号', icon: 'none' }); return }
  if (!password.value) { uni.showToast({ title: '请输入密码', icon: 'none' }); return }

  loading.value = true

  if (isRegister.value) {
    if (!confirmPassword.value) { uni.showToast({ title: '请输入确认密码', icon: 'none' }); loading.value = false; return }
    if (password.value !== confirmPassword.value) { uni.showToast({ title: '两次密码不一致', icon: 'none' }); loading.value = false; return }

    try {
      const res = await request('/auth/register', {
        method: 'POST',
        data: { phone: phone.value, password: password.value, nickname: nickname.value }
      })
      if (res.code === 0) {
        setToken(res.data.token)
        uni.switchTab({ url: '/pages/home/index' })
      } else {
        uni.showToast({ title: res.msg || '注册失败', icon: 'none' })
      }
    } catch (e) {
      uni.showToast({ title: '网络错误', icon: 'none' })
    }
  } else {
    try {
      const res = await request('/auth/login', {
        method: 'POST',
        data: { phone: phone.value, password: password.value }
      })
      if (res.code === 0) {
        setToken(res.data.token)
        uni.switchTab({ url: '/pages/home/index' })
      } else if (res.code === 404) {
        isRegister.value = true
        uni.showToast({ title: '用户不存在，请注册', icon: 'none' })
      } else {
        uni.showToast({ title: res.msg || '登录失败', icon: 'none' })
      }
    } catch (e) {
      uni.showToast({ title: '网络错误', icon: 'none' })
    }
  }

  loading.value = false
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: #f8f9ff;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 16px 20px;
}

.brand-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  background-color: #004ac6;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.logo-text { font-size: 32px; }
.brand-title { font-size: 28px; font-weight: 700; color: #0b1c30; display: block; margin-bottom: 8px; }
.brand-desc { font-size: 16px; color: #434655; }

.form-card {
  width: 100%;
  max-width: 560px;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.form-item { margin-bottom: 16px; }
.form-label { font-size: 14px; font-weight: 600; color: #0b1c30; margin-bottom: 8px; display: block; }

.form-input {
  height: 44px;
  padding: 0 16px;
  border: 1px solid #c3c6d7;
  border-radius: 8px;
  background-color: #e5eeff;
  font-size: 16px;
  color: #0b1c30;
}

.input-with-toggle { display: flex; align-items: center; position: relative; }
.toggle-btn { padding: 0 12px; position: absolute; right: 0; }
.error-text { font-size: 14px; color: #EF4444; margin-top: 4px; display: block; }

.forgot-link { text-align: right; margin-bottom: 16px; }
.forgot-text { font-size: 14px; color: #004ac6; }

.main-btn {
  width: 100%;
  height: 44px;
  background-color: #004ac6;
  color: #ffffff;
  border-radius: 22px;
  font-size: 18px;
  font-weight: 700;
  border: none;
}

.main-btn[disabled] { opacity: 0.6; }

.agreement { margin-top: auto; padding-top: 20px; text-align: center; }
.agreement-text { font-size: 14px; color: #737686; }
</style>
