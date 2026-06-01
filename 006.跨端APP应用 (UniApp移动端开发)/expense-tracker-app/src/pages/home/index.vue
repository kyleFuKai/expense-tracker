<template>
  <view class="home-page">
    <view class="month-bar">
      <text class="month-text">{{ currentYear }}年{{ currentMonth }}月</text>
    </view>

    <view class="overview-card">
      <view class="overview-left">
        <text class="overview-label">本月支出</text>
        <text class="overview-amount">{{ fmtMoney(monthExpense) }}</text>
        <view class="daily-avg-box">
          <text class="avg-label">日均支出</text>
          <text class="avg-value">{{ fmtMoney(dailyAvg) }}</text>
        </view>
      </view>
      <view class="overview-right">
        <text class="overview-label">本月收入</text>
        <text class="income-amount">{{ fmtMoney(monthIncome) }}</text>
        <view v-if="expenseChange !== null" class="change-row">
          <text :class="['change-text', expenseChange >= 0 ? 'text-danger' : 'text-success']">
            {{ expenseChange >= 0 ? '↑' : '↓' }} {{ Math.abs(expenseChange) }}% 较上月
          </text>
        </view>
      </view>
    </view>

    <view class="bill-list">
      <view v-for="(group, dateKey) in groupedBills" :key="dateKey" class="bill-group">
        <view class="bill-date-header">
          <text class="date-text">{{ dateKey }}</text>
          <text class="subtotal-text">{{ group.subtotalSign }}{{ fmtMoney(group.subtotal) }}</text>
        </view>
        <view class="bill-card">
          <view
            v-for="bill in group.bills"
            :key="bill.id"
            class="bill-item"
            @click="goBillDetail(bill.id)"
          >
            <view class="bill-icon-wrap">
              <text class="bill-icon">{{ getCategoryIcon(bill.category_icon) }}</text>
            </view>
            <view class="bill-info">
              <text class="bill-remark">{{ bill.remark || bill.category_name || '其他' }}</text>
              <text class="bill-category">{{ bill.category_name || '其他' }}</text>
              <view v-if="bill.tags && bill.tags.length" class="bill-tags">
                <text v-for="tag in bill.tags.slice(0, 2)" :key="tag.id" class="bill-tag">{{ tag.name }}</text>
                <text v-if="bill.tags.length > 2" class="bill-tag-more">+{{ bill.tags.length - 2 }}</text>
              </view>
            </view>
            <text :class="['bill-amount', bill.type === 'EXPENSE' ? 'text-expense' : 'text-income']">
              {{ bill.type === 'EXPENSE' ? '-' : '+' }}{{ fmtMoney(bill.amount) }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!loading && bills.length === 0" class="empty-state">
      <text class="empty-icon">📋</text>
      <text class="empty-text">本月暂无账单记录</text>
      <text class="empty-hint">点击下方 + 按钮开始记账</text>
    </view>

    <view v-if="loading" class="loading-row">
      <text>加载中...</text>
    </view>

    <view v-if="hasMore && !loading" class="load-more-row" @click="loadMore">
      <text class="load-more-text">加载更多</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getBills, monthlyStats } from '../../api/bills'
import { fmtMoney, getCurrentMonth } from '../../utils/format'

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const monthExpense = ref(0)
const monthIncome = ref(0)
const expenseChange = ref(null)
const dailyAvg = ref(0)
const bills = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const hasMore = ref(true)

function getMonthStr() {
  return currentYear.value + '-' + String(currentMonth.value).padStart(2, '0')
}

function loadStats() {
  monthlyStats(getMonthStr()).then(res => {
    if (res.code !== 0) return
    monthExpense.value = res.data.expense.total
    monthIncome.value = res.data.income.total
    expenseChange.value = res.data.expense.change
    const dayOfMonth = new Date().getDate()
    dailyAvg.value = dayOfMonth > 0 ? monthExpense.value / dayOfMonth : 0
  })
}

function loadBills(reset = false) {
  if (reset) { page.value = 1; bills.value = []; hasMore.value = true }
  if (loading.value) return
  loading.value = true

  getBills({ month: getMonthStr(), page: page.value, pageSize }).then(res => {
    loading.value = false
    if (res.code !== 0) return
    const data = res.data
    if (reset) bills.value = data.list
    else bills.value = bills.value.concat(data.list)
    hasMore.value = data.list.length >= pageSize
    if (data.list.length < pageSize) hasMore.value = false
  })
}

function loadMore() {
  if (hasMore.value && !loading.value) {
    page.value++
    loadBills()
  }
}

const groupedBills = computed(() => {
  const groups = {}
  bills.value.forEach(bill => {
    const dateKey = bill.bill_time ? bill.bill_time.substring(0, 10).replace(/-/g, '/') : '未知日期'
    if (!groups[dateKey]) {
      groups[dateKey] = { bills: [], subtotal: 0, subtotalSign: '' }
    }
    groups[dateKey].bills.push(bill)
    if (bill.type === 'EXPENSE') groups[dateKey].subtotal -= parseFloat(bill.amount)
    else groups[dateKey].subtotal += parseFloat(bill.amount)
  })
  Object.values(groups).forEach(g => {
    g.subtotalSign = g.subtotal >= 0 ? '+' : '-'
    g.subtotal = Math.abs(g.subtotal)
  })
  return groups
})

function getCategoryIcon(icon) {
  const icons = {
    restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️',
    movie: '🎬', home: '🏠', medical_services: '💊',
    school: '📚', group: '👥', payments: '💰',
    more_horiz: '📝', default: '📝'
  }
  return icons[icon] || icons.default
}

function goBillDetail(id) {
  uni.navigateTo({ url: `/pages/bill-detail/index?id=${id}` })
}

onShow(() => {
  loadStats()
  loadBills(true)
})
</script>

<style scoped>
.home-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }
.month-bar { margin-bottom: 16px; }
.month-text { font-size: 20px; font-weight: 600; color: #0b1c30; }

.overview-card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.overview-label { font-size: 12px; color: #434655; display: block; margin-bottom: 4px; }
.overview-amount { font-size: 28px; font-weight: 700; color: #0b1c30; }
.income-amount { font-size: 20px; font-weight: 600; color: #006c49; }

.daily-avg-box { background-color: #e5eeff; border-radius: 8px; padding: 8px 12px; margin-top: 12px; }
.avg-label { font-size: 10px; color: #434655; display: block; }
.avg-value { font-size: 16px; color: #0b1c30; }

.change-row { margin-top: 8px; }
.change-text { font-size: 14px; }
.text-danger { color: #EF4444; }
.text-success { color: #10B981; }

.bill-date-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; }
.date-text { font-size: 12px; color: #434655; background-color: #e5eeff; padding: 4px 12px; border-radius: 12px; }
.subtotal-text { font-size: 12px; color: #434655; }

.bill-card { background-color: #ffffff; border-radius: 12px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
.bill-item { display: flex; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e5eeff; }
.bill-item:last-child { border-bottom: none; }

.bill-icon-wrap { width: 40px; height: 40px; border-radius: 20px; background-color: #e5eeff; display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.bill-icon { font-size: 20px; }
.bill-info { flex: 1; }
.bill-remark { font-size: 16px; color: #0b1c30; display: block; }
.bill-category { font-size: 12px; color: #434655; }

.bill-tags { display: flex; gap: 4px; margin-top: 4px; }
.bill-tag { font-size: 11px; background-color: #f5f5f5; color: #434655; padding: 2px 8px; border-radius: 8px; }
.bill-tag-more { font-size: 11px; color: #434655; }

.bill-amount { font-size: 16px; font-weight: 500; }
.text-expense { color: #EF4444; }
.text-income { color: #10B981; }

.empty-state { text-align: center; padding: 40px 0; }
.empty-icon { font-size: 48px; display: block; margin-bottom: 12px; }
.empty-text { font-size: 16px; color: #434655; display: block; }
.empty-hint { font-size: 14px; color: #737686; display: block; margin-top: 4px; }

.loading-row, .load-more-row { text-align: center; padding: 12px; color: #434655; font-size: 14px; }
</style>
