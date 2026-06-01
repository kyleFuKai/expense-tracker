<template>
  <view class="settings-page">
    <view class="profile-card">
      <view class="avatar">
        <text class="avatar-text">{{ nickname[0] || '👤' }}</text>
      </view>
      <view class="profile-info">
        <text class="nickname">{{ nickname || '用户' }}</text>
        <text class="phone">{{ phone }}</text>
      </view>
    </view>

    <view class="menu-list">
      <view class="menu-item" @click="goCategoryManage">
        <text class="menu-icon">📂</text>
        <text class="menu-text">类别管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goBudget">
        <text class="menu-icon">💰</text>
        <text class="menu-text">预算设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goChangePassword">
        <text class="menu-icon">🔒</text>
        <text class="menu-text">修改密码</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <button class="logout-btn" @click="doLogout">退出登录</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserProfile } from '../../api/user'
import { logout } from '../../utils/storage'

const nickname = ref('')
const phone = ref('')

function goCategoryManage() { uni.navigateTo({ url: '/pages/category-manage/index' }) }
function goBudget() { uni.navigateTo({ url: '/pages/budget/index' }) }
function goChangePassword() { uni.navigateTo({ url: '/pages/change-password/index' }) }

function doLogout() {
  uni.showModal({ title: '确认退出', content: '确定要退出登录吗？', success: (res) => {
    if (res.confirm) logout()
  }})
}

onShow(() => {
  getUserProfile().then(res => {
    if (res.code === 0) {
      nickname.value = res.data.nickname || ''
      phone.value = res.data.phone || ''
    }
  })
})
</script>

<style scoped>
.settings-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }

.profile-card { background-color: #ffffff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.avatar { width: 60px; height: 60px; border-radius: 30px; background-color: #e5eeff; display: flex; align-items: center; justify-content: center; }
.avatar-text { font-size: 28px; }
.profile-info { flex: 1; }
.nickname { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; }
.phone { font-size: 14px; color: #434655; }

.menu-list { background-color: #ffffff; border-radius: 12px; margin-bottom: 16px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.menu-item { display: flex; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e5eeff; }
.menu-item:last-child { border-bottom: none; }
.menu-icon { font-size: 24px; margin-right: 12px; }
.menu-text { flex: 1; font-size: 16px; color: #0b1c30; }
.menu-arrow { font-size: 20px; color: #c3c6d7; }

.logout-btn { width: 100%; height: 44px; border: 1px solid #c3c6d7; color: #EF4444; border-radius: 12px; font-size: 16px; background-color: #ffffff; }
</style>
