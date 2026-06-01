<template>
  <view class="budget-page">
    <view v-if="loading" class="loading-state"><text>加载中...</text></view>
    <view v-else>
      <view class="progress-card">
        <view class="progress-header">
          <view>
            <text class="progress-label">本月支出进度</text>
            <text class="progress-amount">{{ fmtMoney(spent) }}</text>
          </view>
          <view>
            <text class="progress-label">总预算</text>
            <text class="budget-text">{{ fmtMoney(totalBudget) }}</text>
          </view>
        </view>
        <view class="progress-bar-bg">
          <view class="progress-bar" :class="progressClass" :style="{ width: Math.min(percent, 100) + '%' }"></view>
        </view>
        <view class="progress-footer">
          <text class="progress-pct">已使用 {{ percent }}%</text>
          <text :class="['remaining-text', remaining >= 0 ? 'text-success' : 'text-danger']">
            {{ remaining >= 0 ? '剩余 ' + fmtMoney(remaining) : '超出 ' + fmtMoney(Math.abs(remaining)) }}
          </text>
        </view>
      </view>

      <view class="budget-input-section">
        <text class="input-label">设置本月总预算</text>
        <view class="budget-input-row">
          <view class="yen-input-wrap">
            <text class="yen-symbol">¥</text>
            <input class="yen-input" v-model="totalBudgetInput" type="digit" placeholder="0.00" />
          </view>
          <button class="save-budget-btn" @click="saveTotalBudget" :disabled="savingBudget">{{ savingBudget ? '保存中...' : '保存' }}</button>
        </view>
      </view>

      <view class="category-section" v-if="categoryBudgets.length">
        <text class="section-title">分类预算</text>
        <view v-for="cat in categoryBudgets" :key="cat.budget_id" class="category-budget-card" :class="{ 'over-budget': cat.percent > 100 }">
          <view class="cat-budget-header">
            <view class="cat-budget-info">
              <view class="cat-icon-wrap"><text>{{ getCategoryEmoji(cat.category_icon) }}</text></view>
              <view>
                <text class="cat-budget-name">{{ cat.category_name }}</text>
                <text :class="['cat-budget-amount', cat.percent > 100 ? 'text-danger' : '']">
                  {{ cat.percent > 100 ? '超出 ' + fmtMoney(Math.abs(cat.remaining)) : fmtMoney(cat.spent) + ' / ' + fmtMoney(cat.budget_amount) }}
                </text>
              </view>
            </view>
            <view class="cat-budget-actions">
              <text class="cat-budget-pct">{{ cat.percent }}%</text>
              <text class="cat-edit" @click="openEditDialog(cat)">✏️</text>
              <text class="cat-delete" @click="deleteCatBudget(cat)">🗑️</text>
            </view>
          </view>
          <view class="cat-progress-bg">
            <view class="cat-progress-bar" :class="cat.percent > 100 ? 'bg-danger' : cat.percent >= 80 ? 'bg-warning' : 'bg-primary'" :style="{ width: Math.min(cat.percent, 100) + '%' }"></view>
          </view>
          <view v-if="cat.percent > 100" class="over-text">
            <text>⚠️ 超出预算 {{ cat.percent - 100 }}%</text>
          </view>
        </view>
      </view>

      <view v-if="!categoryBudgets.length" class="empty-budget">
        <text class="empty-icon">📊</text>
        <text class="empty-text">暂无分类预算</text>
        <text class="empty-hint">点击下方按钮添加分类预算</text>
      </view>

      <button class="add-cat-budget-btn" @click="showAddDialog">➕ 添加分类预算</button>
    </view>

    <view v-if="showDialog" class="dialog-overlay" @click="showDialog = false">
      <view class="dialog" @click.stop>
        <text class="dialog-title">{{ editBudgetId ? '修改分类预算' : '添加分类预算' }}</text>
        <picker :range="categoryOptions" range-key="name" @change="onCategoryPick" :value="selectedCatIdx">
          <view class="picker-btn"><text>{{ selectedCatName || '请选择分类' }}</text></view>
        </picker>
        <input class="dialog-input" v-model="dialogAmount" type="digit" placeholder="月度预算金额" />
        <view class="dialog-btns">
          <button class="dialog-cancel" @click="showDialog = false">取消</button>
          <button class="dialog-ok" @click="saveCatBudget">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getDashboard, createBudget, deleteBudget } from '../../api/budgets'
import { getCategories } from '../../api/categories'
import { request } from '../../api/request'
import { fmtMoney, getCurrentMonth } from '../../utils/format'

const loading = ref(true)
const spent = ref(0)
const totalBudget = ref(0)
const percent = ref(0)
const remaining = ref(0)
const categoryBudgets = ref([])
const totalBudgetInput = ref('')
const savingBudget = ref(false)

const showDialog = ref(false)
const editBudgetId = ref(null)
const dialogAmount = ref('')
const selectedCatId = ref(null)
const selectedCatIdx = ref(0)
const categoryOptions = ref([])

function getCategoryEmoji(icon) {
  const map = { restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️', movie: '🎬', home: '🏠', medical_services: '💊', school: '📚', group: '👥', payments: '💰', more_horiz: '📝', default: '📝' }
  return map[icon] || map.default
}

const progressClass = computed(() => {
  if (percent.value > 100) return 'bg-danger'
  if (percent.value >= 80) return 'bg-warning'
  return 'bg-primary'
})

const selectedCatName = computed(() => {
  const cat = categoryOptions.value[selectedCatIdx.value]
  return cat ? cat.name : '请选择分类'
})

function loadDashboard() {
  loading.value = true
  getDashboard(getCurrentMonth()).then(res => {
    loading.value = false
    if (res.code !== 0) return
    spent.value = res.data.spent
    totalBudget.value = res.data.total_budget
    percent.value = res.data.percent
    remaining.value = res.data.remaining
    categoryBudgets.value = res.data.categories || []
    totalBudgetInput.value = res.data.total_budget > 0 ? res.data.total_budget : ''
  })
}

function saveTotalBudget() {
  const val = parseFloat(totalBudgetInput.value)
  if (!val || val <= 0) { uni.showToast({ title: '请输入有效金额', icon: 'none' }); return }
  savingBudget.value = true
  request('/budgets', { method: 'POST', data: { category_id: 0, amount: val, period: 'MONTHLY' } })
    .then(res => { savingBudget.value = false; if (res.code === 0) loadDashboard(); else uni.showToast({ title: res.msg || '保存失败', icon: 'none' }) })
    .catch(() => { savingBudget.value = false; uni.showToast({ title: '网络错误', icon: 'none' }) })
}

function loadExpenseCategories() {
  getCategories('EXPENSE').then(res => {
    if (res.code !== 0) return
    categoryOptions.value = res.data
  })
}

function showAddDialog() {
  editBudgetId.value = null
  dialogAmount.value = ''
  selectedCatIdx.value = 0
  loadExpenseCategories()
  showDialog.value = true
}

function openEditDialog(cat) {
  editBudgetId.value = cat.budget_id
  loadExpenseCategories()
  dialogAmount.value = cat.budget_amount
  showDialog.value = true
  setTimeout(() => {
    const idx = categoryOptions.value.findIndex(c => c.id === cat.cat_id)
    if (idx >= 0) selectedCatIdx.value = idx
  }, 100)
}

function onCategoryPick(e) { selectedCatIdx.value = e.detail.value }

function saveCatBudget() {
  const catId = categoryOptions.value[selectedCatIdx.value]?.id
  const amount = parseFloat(dialogAmount.value)
  if (!catId) { uni.showToast({ title: '请选择分类', icon: 'none' }); return }
  if (!amount || amount <= 0) { uni.showToast({ title: '请输入有效金额', icon: 'none' }); return }
  createBudget({ category_id: catId, amount, period: 'MONTHLY' }).then(res => {
    if (res.code === 0) { showDialog.value = false; loadDashboard() }
    else { uni.showToast({ title: res.msg || '保存失败', icon: 'none' }) }
  })
}

function deleteCatBudget(cat) {
  uni.showModal({ title: '确认停用', content: '确认停用此分类预算？', success: async (res) => {
    if (res.confirm) { await deleteBudget(cat.budget_id); loadDashboard() }
  }})
}

onShow(() => { loadDashboard() })
</script>

<style scoped>
.budget-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }
.loading-state { text-align: center; padding: 40px; color: #434655; }

.progress-card { background-color: #ffffff; border-radius: 12px; padding: 20px; margin-bottom: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.progress-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.progress-label { font-size: 12px; color: #434655; display: block; margin-bottom: 4px; }
.progress-amount { font-size: 28px; font-weight: 700; color: #0b1c30; }
.budget-text { font-size: 20px; font-weight: 600; color: #004ac6; }
.progress-bar-bg { width: 100%; height: 12px; background-color: #e5eeff; border-radius: 6px; overflow: hidden; margin-bottom: 8px; }
.progress-bar { height: 100%; border-radius: 6px; transition: width 0.5s; }
.bg-primary { background-color: #004ac6; }
.bg-warning { background-color: #F59E0B; }
.bg-danger { background-color: #EF4444; }
.progress-footer { display: flex; justify-content: space-between; }
.progress-pct { font-size: 14px; color: #434655; }
.remaining-text { font-size: 14px; font-weight: 600; }
.text-success { color: #10B981; }
.text-danger { color: #EF4444; }

.budget-input-section { margin-bottom: 16px; }
.input-label { font-size: 12px; color: #434655; display: block; margin-bottom: 8px; }
.budget-input-row { display: flex; gap: 8px; }
.yen-input-wrap { flex: 1; display: flex; align-items: center; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 12px; padding: 0 12px; }
.yen-symbol { font-size: 20px; color: #434655; margin-right: 4px; }
.yen-input { flex: 1; font-size: 20px; font-weight: 600; border: none; padding: 0; color: #0b1c30; }
.save-budget-btn { background-color: #004ac6; color: #ffffff; border-radius: 12px; padding: 0 16px; font-size: 14px; font-weight: 600; border: none; height: 44px; }

.category-section { margin-bottom: 12px; }
.section-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; margin-bottom: 12px; }
.category-budget-card { background-color: #ffffff; border-radius: 12px; padding: 16px; margin-bottom: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
.category-budget-card.over-budget { border: 1px solid rgba(239,68,68,0.2); }
.cat-budget-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.cat-budget-info { display: flex; align-items: center; gap: 12px; }
.cat-icon-wrap { width: 40px; height: 40px; border-radius: 20px; background-color: #e5eeff; display: flex; align-items: center; justify-content: center; }
.cat-budget-name { font-size: 16px; font-weight: 600; color: #0b1c30; display: block; }
.cat-budget-amount { font-size: 14px; color: #434655; }
.cat-budget-actions { display: flex; align-items: center; gap: 8px; }
.cat-budget-pct { font-size: 14px; color: #434655; }
.cat-edit, .cat-delete { font-size: 18px; cursor: pointer; }
.cat-delete { color: #EF4444; }
.cat-progress-bg { width: 100%; height: 8px; background-color: #e5eeff; border-radius: 4px; overflow: hidden; }
.cat-progress-bar { height: 100%; border-radius: 4px; }
.over-text { margin-top: 4px; font-size: 12px; color: #EF4444; font-weight: 600; }

.empty-budget { text-align: center; padding: 32px 0; }
.empty-icon { font-size: 48px; display: block; margin-bottom: 8px; }
.empty-text { font-size: 16px; color: #434655; display: block; }
.empty-hint { font-size: 14px; color: #737686; display: block; margin-top: 4px; }

.add-cat-budget-btn { width: 100%; height: 44px; border: 2px dashed #c3c6d7; border-radius: 12px; background: none; color: #434655; font-size: 14px; }

.dialog-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0,0,0,0.4); display: flex; align-items: flex-end; z-index: 100; }
.dialog { width: 100%; background-color: #ffffff; border-radius: 16px 16px 0 0; padding: 20px; }
.dialog-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; margin-bottom: 16px; }
.picker-btn { height: 44px; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 12px; display: flex; align-items: center; margin-bottom: 12px; }
.dialog-input { height: 44px; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 12px; font-size: 16px; margin-bottom: 12px; }
.dialog-btns { display: flex; gap: 12px; }
.dialog-cancel, .dialog-ok { flex: 1; height: 44px; border-radius: 12px; font-size: 16px; border: none; }
.dialog-cancel { border: 1px solid #c3c6d7; color: #434655; }
.dialog-ok { background-color: #004ac6; color: #ffffff; }
</style>
