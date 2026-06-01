<template>
  <view class="manage-page">
    <view class="type-tabs">
      <view :class="['type-tab', currentType === 'expense' ? 'active' : '']" @click="currentType = 'expense'; loadCategories()">支出</view>
      <view :class="['type-tab', currentType === 'income' ? 'active' : '']" @click="currentType = 'income'; loadCategories()">收入</view>
    </view>

    <view class="category-list">
      <view v-for="cat in categories" :key="cat.id" class="category-item" @click="showActions(cat)">
        <text class="cat-icon">{{ cat.icon === 'more_horiz' ? '📝' : getCategoryIcon(cat.icon) }}</text>
        <text class="cat-name">{{ cat.name }}</text>
        <text v-if="cat.is_preset" class="cat-badge">预设</text>
        <text v-else class="cat-badge custom">自定义</text>
      </view>
    </view>

    <button class="add-btn" @click="showAddDialog">
      <text>➕ 添加自定义分类</text>
    </button>

    <view v-if="showDialog" class="dialog-overlay" @click="showDialog = false">
      <view class="dialog" @click.stop>
        <text class="dialog-title">添加自定义分类</text>
        <input class="dialog-input" v-model="dialogName" placeholder="分类名称" maxlength="16" />
        <view class="icon-picker">
          <text v-for="icon in iconOptions" :key="icon" :class="['icon-option', selectedIcon === icon ? 'icon-selected' : '']" @click="selectedIcon = icon">{{ icon }}</text>
        </view>
        <view class="dialog-btns">
          <button class="dialog-cancel" @click="showDialog = false">取消</button>
          <button class="dialog-ok" @click="saveCategory">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCategories, createCategory, deleteCategory } from '../../api/categories'

const currentType = ref('expense')
const categories = ref([])
const showDialog = ref(false)
const dialogName = ref('')
const selectedIcon = ref('📝')

const iconOptions = ref(['🍽️', '🚗', '🛍️', '🎬', '🏠', '💊', '📚', '👥', '💰', '🎮', '🐱', '📱', '💰', '✈️', '🏋️', '☕', '🍪', '🥐', '🍽️', '🚕', '🅿️', '⛽', '🚇', '👗', '👁️', '📱', '📝'])

function getCategoryIcon(icon) {
  const map = { restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️', movie: '🎬', home: '🏠', medical_services: '💊', school: '📚', group: '👥', payments: '💰', more_horiz: '📝', sports_esports: '🎮', pets: '🐱', phone_android: '📱', savings: '💰', travel_explore: '✈️', fitness_center: '🏋️', coffee: '☕', cookie: '🍪', bakery_dining: '🥐', dining: '🍽️', local_taxi: '🚕', local_parking: '🅿️', local_gas_station: '⛽', subway: '🚇', checkroom: '👗', visibility: '👁️', devices: '📱', flight: '✈️', add: '📝', default: '📝' }
  return map[icon] || map.default
}

function loadCategories() {
  getCategories(currentType.value.toUpperCase()).then(res => {
    if (res.code !== 0) return
    categories.value = res.data
  })
}

function showActions(cat) {
  if (cat.is_preset) { uni.showToast({ title: '系统预设分类不可修改', icon: 'none' }); return }
  uni.showModal({
    title: '删除分类',
    content: `确认删除分类 "${cat.name}"？`,
    success: async (res) => {
      if (res.confirm) {
        const result = await deleteCategory(cat.id)
        if (result.code === 0) { loadCategories() }
        else { uni.showToast({ title: result.msg || '删除失败', icon: 'none' }) }
      }
    }
  })
}

function showAddDialog() { dialogName.value = ''; selectedIcon.value = '📝'; showDialog.value = true }

async function saveCategory() {
  const name = dialogName.value.trim()
  if (!name) { uni.showToast({ title: '请输入分类名称', icon: 'none' }); return }
  const result = await createCategory({ name, icon: selectedIcon.value, type: currentType.value.toUpperCase() })
  if (result.code === 0) { showDialog.value = false; loadCategories() }
  else { uni.showToast({ title: result.msg || '添加失败', icon: 'none' }) }
}

onShow(() => { loadCategories() })
</script>

<style scoped>
.manage-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }
.type-tabs { display: flex; background-color: #e5eeff; border-radius: 12px; padding: 4px; margin-bottom: 16px; }
.type-tab { flex: 1; text-align: center; padding: 8px; font-size: 14px; border-radius: 8px; color: #434655; }
.type-tab.active { background-color: #ffffff; color: #004ac6; box-shadow: 0 1px 3px rgba(0,0,0,0.1); font-weight: 600; }

.category-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.category-item { background-color: #ffffff; border-radius: 16px; padding: 16px; display: flex; flex-direction: column; align-items: center; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
.cat-icon { font-size: 32px; margin-bottom: 8px; }
.cat-name { font-size: 14px; color: #0b1c30; font-weight: 600; }
.cat-badge { font-size: 10px; color: #434655; margin-top: 4px; }
.cat-badge.custom { color: #004ac6; }

.add-btn { width: 100%; height: 44px; border: 2px dashed #c3c6d7; border-radius: 12px; background: none; color: #434655; font-size: 14px; }

.dialog-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0,0,0,0.4); display: flex; align-items: flex-end; z-index: 100; }
.dialog { width: 100%; background-color: #ffffff; border-radius: 16px 16px 0 0; padding: 20px; }
.dialog-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; margin-bottom: 16px; }
.dialog-input { height: 44px; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 12px; font-size: 16px; margin-bottom: 12px; }
.icon-picker { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.icon-option { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border: 1px solid #c3c6d7; border-radius: 8px; font-size: 20px; }
.icon-selected { border-color: #004ac6; background-color: #dbeafe; }
.dialog-btns { display: flex; gap: 12px; }
.dialog-cancel, .dialog-ok { flex: 1; height: 44px; border-radius: 12px; font-size: 16px; border: none; }
.dialog-cancel { border: 1px solid #c3c6d7; color: #434655; }
.dialog-ok { background-color: #004ac6; color: #ffffff; }
</style>
