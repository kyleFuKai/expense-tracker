<template>
  <view class="change-page">
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">旧密码</text>
        <input class="form-input" v-model="oldPassword" type="password" placeholder="请输入旧密码" />
      </view>
      <view class="form-item">
        <text class="form-label">新密码</text>
        <input class="form-input" v-model="newPassword" type="password" placeholder="请输入新密码" maxlength="20" />
      </view>
      <view class="form-item">
        <text class="form-label">确认新密码</text>
        <input class="form-input" v-model="confirmPassword" type="password" placeholder="请再次输入新密码" maxlength="20" />
      </view>
      <text v-if="mismatch" class="error-text">两次输入的密码不一致</text>
      <button class="submit-btn" @click="submitChange" :disabled="saving">{{ saving ? '修改中...' : '确认修改' }}</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { changePassword } from '../../api/user'

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const saving = ref(false)

const mismatch = computed(() => confirmPassword.value && newPassword.value !== confirmPassword.value)

async function submitChange() {
  if (!oldPassword.value) { uni.showToast({ title: '请输入旧密码', icon: 'none' }); return }
  if (!newPassword.value) { uni.showToast({ title: '请输入新密码', icon: 'none' }); return }
  if (newPassword.value !== confirmPassword.value) { uni.showToast({ title: '两次密码不一致', icon: 'none' }); return }

  saving.value = true
  try {
    const res = await changePassword({ old_password: oldPassword.value, new_password: newPassword.value })
    if (res.code === 0) {
      uni.showToast({ title: '修改成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1500)
    } else {
      uni.showToast({ title: res.msg || '修改失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
  saving.value = false
}
</script>

<style scoped>
.change-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }
.form-card { background-color: #ffffff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.form-item { margin-bottom: 16px; }
.form-label { font-size: 14px; font-weight: 600; color: #0b1c30; margin-bottom: 8px; display: block; }
.form-input { height: 44px; padding: 0 16px; border: 1px solid #c3c6d7; border-radius: 8px; background-color: #e5eeff; font-size: 16px; color: #0b1c30; }
.error-text { font-size: 14px; color: #EF4444; margin-bottom: 12px; display: block; }
.submit-btn { width: 100%; height: 44px; background-color: #004ac6; color: #ffffff; border-radius: 22px; font-size: 16px; font-weight: 700; border: none; }
.submit-btn[disabled] { opacity: 0.6; }
</style>
