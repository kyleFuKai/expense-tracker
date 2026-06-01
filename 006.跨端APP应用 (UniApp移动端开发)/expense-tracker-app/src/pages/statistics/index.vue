<template>
  <view class="stats-page">
    <view class="month-switcher">
      <view class="month-btn" @click="prevMonth">◀</view>
      <view class="month-title-wrap">
        <text class="month-title">{{ currentYear }}年{{ currentMonth }}月</text>
        <text class="month-sub">月度概览</text>
      </view>
      <view class="month-btn" @click="nextMonth" v-if="canNext">▶</view>
    </view>

    <view class="summary-row">
      <view class="summary-card">
        <text class="summary-label">总支出</text>
        <text class="summary-amount">{{ fmtMoney(totalExpense) }}</text>
        <text class="summary-count">{{ expenseCount }} 笔支出</text>
        <view v-if="expenseChange !== null">
          <text :class="['change-text', expenseChange >= 0 ? 'text-danger' : 'text-success']">
            {{ expenseChange >= 0 ? '↑' : '↓' }} {{ Math.abs(expenseChange) }}% 较上月
          </text>
        </view>
      </view>
      <view class="summary-card">
        <text class="summary-label">总收入</text>
        <text class="income-amount">{{ fmtMoney(totalIncome) }}</text>
        <text class="summary-count">{{ incomeCount }} 笔收入</text>
        <view v-if="incomeChange !== null">
          <text :class="['change-text', incomeChange >= 0 ? 'text-success' : 'text-danger']">
            {{ incomeChange >= 0 ? '↑' : '↓' }} {{ Math.abs(incomeChange) }}% 较上月
          </text>
        </view>
      </view>
    </view>

    <view class="chart-card">
      <text class="chart-title">支出趋势</text>
      <text class="chart-sub">每日支出柱状图</text>
      <view class="chart-bars">
        <view v-for="(d, idx) in dailyData" :key="idx" class="chart-bar-wrap">
          <view class="chart-bar" :style="{ height: (d.expense / maxExpense * 80) + 'px' }"></view>
          <text class="chart-day">{{ d.date ? d.date.substring(8) + '日' : '' }}</text>
        </view>
      </view>
    </view>

    <view class="ranking-card">
      <text class="ranking-title">支出排行</text>
      <view v-for="(cat, idx) in categories.slice(0, 5)" :key="cat.id" class="ranking-item">
        <view class="ranking-left">
          <view :class="['rank-icon-wrap', `rank-color-${idx % 5}`]">
            <text class="rank-icon">{{ getCategoryEmoji(cat.icon) }}</text>
          </view>
          <view>
            <text class="rank-name">{{ cat.name }}</text>
            <text class="rank-count">{{ cat.count }} 笔</text>
          </view>
        </view>
        <view class="ranking-right">
          <text class="rank-amount">¥{{ parseFloat(cat.total).toFixed(2) }}</text>
          <text class="rank-pct">{{ catPct(cat.total) }}% 占比</text>
        </view>
      </view>
      <view v-if="!categories.length" class="empty-ranking">
        <text>暂无数据</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { monthlyStats } from '../../api/bills'
import { fmtMoney } from '../../utils/format'

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const totalExpense = ref(0)
const totalIncome = ref(0)
const expenseCount = ref(0)
const incomeCount = ref(0)
const expenseChange = ref(null)
const incomeChange = ref(null)
const dailyData = ref([])
const categories = ref([])

function getMonthStr() {
  return currentYear.value + '-' + String(currentMonth.value).padStart(2, '0')
}

const canNext = computed(() => {
  const now = new Date()
  return !(currentYear.value > now.getFullYear() || (currentYear.value === now.getFullYear() && currentMonth.value > now.getMonth() + 1))
})

const maxExpense = computed(() => {
  let max = 0
  dailyData.value.forEach(d => { if (d.expense > max) max = d.expense })
  return max || 1
})

function catPct(total) {
  const sum = categories.value.reduce((s, c) => s + parseFloat(c.total), 0)
  return sum > 0 ? (parseFloat(total) / sum * 100).toFixed(0) : 0
}

function getCategoryEmoji(icon) {
  const map = {
    restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️',
    movie: '🎬', home: '🏠', medical_services: '💊',
    school: '📚', group: '👥', payments: '💰',
    more_horiz: '📝', default: '📝'
  }
  return map[icon] || map.default
}

function loadStats() {
  monthlyStats(getMonthStr()).then(res => {
    if (res.code !== 0) return
    totalExpense.value = res.data.expense.total
    expenseCount.value = res.data.expense.count
    expenseChange.value = res.data.expense.change
    totalIncome.value = res.data.income.total
    incomeCount.value = res.data.income.count
    incomeChange.value = res.data.income.change
    dailyData.value = res.data.daily.slice(0, 15).reverse()
    categories.value = res.data.categories
  })
}

function prevMonth() {
  currentMonth.value--
  if (currentMonth.value < 1) { currentMonth.value = 12; currentYear.value-- }
  loadStats()
}

function nextMonth() {
  if (!canNext.value) return
  currentMonth.value++
  if (currentMonth.value > 12) { currentMonth.value = 1; currentYear.value++ }
  loadStats()
}

onShow(() => { loadStats() })
</script>

<style scoped>
.stats-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }

.month-switcher { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.month-btn { width: 36px; height: 36px; background-color: #ffffff; border: 1px solid #c3c6d7; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.month-title-wrap { text-align: center; }
.month-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; }
.month-sub { font-size: 12px; color: #434655; }

.summary-row { display: flex; gap: 12px; margin-bottom: 16px; }
.summary-card { flex: 1; background-color: #ffffff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.summary-label { font-size: 12px; color: #434655; display: block; margin-bottom: 4px; }
.summary-amount { font-size: 28px; font-weight: 700; color: #0b1c30; display: block; }
.income-amount { font-size: 20px; font-weight: 600; color: #006c49; display: block; }
.summary-count { font-size: 14px; color: #434655; display: block; margin-top: 8px; }

.change-text { font-size: 14px; display: block; margin-top: 4px; }
.text-danger { color: #EF4444; }
.text-success { color: #10B981; }

.chart-card { background-color: #ffffff; border-radius: 12px; padding: 16px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.chart-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; }
.chart-sub { font-size: 14px; color: #434655; display: block; margin-bottom: 16px; }
.chart-bars { display: flex; align-items: flex-end; height: 100px; gap: 2px; }
.chart-bar-wrap { flex: 1; display: flex; flex-direction: column; align-items: center; }
.chart-bar { width: 100%; background-color: #004ac6; border-radius: 3px 3px 0 0; min-height: 4px; }
.chart-day { font-size: 9px; color: #434655; margin-top: 2px; }

.ranking-card { background-color: #ffffff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.ranking-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; margin-bottom: 16px; }
.ranking-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5eeff; }
.ranking-item:last-child { border-bottom: none; }
.ranking-left { display: flex; align-items: center; gap: 12px; }
.rank-icon-wrap { width: 40px; height: 40px; border-radius: 20px; display: flex; align-items: center; justify-content: center; }
.rank-color-0 { background-color: #dbeafe; }
.rank-color-1 { background-color: #fee2e2; }
.rank-color-2 { background-color: #dcfce7; }
.rank-color-3 { background-color: #fef3c7; }
.rank-color-4 { background-color: #ede9fe; }
.rank-icon { font-size: 20px; }
.rank-name { font-size: 16px; font-weight: 600; color: #0b1c30; display: block; }
.rank-count { font-size: 14px; color: #434655; }
.ranking-right { text-align: right; }
.rank-amount { font-size: 16px; font-weight: 700; color: #0b1c30; display: block; }
.rank-pct { font-size: 12px; color: #434655; }
.empty-ranking { text-align: center; padding: 32px; color: #434655; }
</style>
