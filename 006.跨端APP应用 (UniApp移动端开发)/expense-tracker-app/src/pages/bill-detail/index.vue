<template>
  <view class="detail-page">
    <view v-if="loading" class="loading-state">
      <text>加载中...</text>
    </view>
    <view v-else-if="bill" class="bill-card">
      <view class="card-header">
        <text class="bill-icon">{{ getCategoryIcon(bill.category_icon) }}</text>
        <text class="bill-category-name">{{ bill.category_name || '其他' }}</text>
        <text :class="['bill-amount', bill.type === 'EXPENSE' ? 'text-expense' : 'text-income']">
          {{ bill.type === 'EXPENSE' ? '-' : '+' }}{{ fmtMoney(bill.amount) }}
        </text>
      </view>

      <view class="detail-row">
        <text class="detail-label">交易时间</text>
        <text class="detail-value">{{ fmtDate(bill.bill_time) }}</text>
      </view>

      <view class="detail-row">
        <text class="detail-label">类型</text>
        <text class="detail-value">{{ bill.type === 'EXPENSE' ? '支出' : '收入' }}</text>
      </view>

      <view v-if="bill.remark" class="remark-box">
        <view class="remark-header">
          <text>📝</text>
          <text class="remark-label">备注</text>
        </view>
        <text class="remark-text">{{ bill.remark }}</text>
      </view>

      <view v-if="bill.tags && bill.tags.length" class="tags-box">
        <view class="tags-header">
          <text>🏷️</text>
          <text class="tags-label">标签</text>
        </view>
        <view class="tags-row">
          <text v-for="tag in bill.tags" :key="tag.id" class="tag-chip">{{ tag.name }}</text>
        </view>
      </view>
    </view>

    <view v-if="bill" class="action-btns">
      <button class="edit-btn" @click="editBill">
        <text>✏️ 修改账单</text>
      </button>
      <button class="delete-btn" @click="deleteBill">
        <text>🗑️ 删除账单</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getBillById, deleteBill as deleteBillApi } from '../../api/bills'
import { fmtMoney, fmtDate } from '../../utils/format'

const bill = ref(null)
const loading = ref(true)

function getCategoryIcon(icon) {
  const map = { restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️', movie: '🎬', home: '🏠', medical_services: '💊', school: '📚', group: '👥', payments: '💰', more_horiz: '📝', default: '📝' }
  return map[icon] || map.default
}

function editBill() {
  uni.navigateTo({ url: `/pages/record/index?edit=${bill.value.id}` })
}

async function deleteBill() {
  uni.showModal({
    title: '确认删除',
    content: '确认删除此账单？',
    success: async (res) => {
      if (res.confirm) {
        const result = await deleteBillApi(bill.value.id)
        if (result.code === 0) {
          uni.navigateBack()
        } else {
          uni.showToast({ title: result.msg || '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onLoad((query) => {
  if (query.id) {
    getBillById(query.id).then(res => {
      loading.value = false
      if (res.code === 0) bill.value = res.data
    })
  }
})
</script>

<style scoped>
.detail-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }
.loading-state { text-align: center; padding: 40px; color: #434655; }

.bill-card { background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); overflow: hidden; }

.card-header { display: flex; flex-direction: column; align-items: center; padding: 20px; border-bottom: 1px dashed #c3c6d7; }
.bill-icon { font-size: 48px; margin-bottom: 8px; }
.bill-category-name { font-size: 20px; font-weight: 600; color: #434655; margin-bottom: 4px; }
.bill-amount { font-size: 28px; font-weight: 700; }
.text-expense { color: #EF4444; }
.text-income { color: #10B981; }

.detail-row { display: flex; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #e5eeff; }
.detail-label { font-size: 12px; color: #434655; font-weight: 600; }
.detail-value { font-size: 16px; color: #0b1c30; }

.remark-box { margin: 16px 20px; background-color: #e5eeff; padding: 16px; border-radius: 8px; }
.remark-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.remark-label { font-size: 12px; color: #434655; font-weight: 600; }
.remark-text { font-size: 16px; color: #0b1c30; font-style: italic; }

.tags-box { margin: 0 20px 16px; background-color: #e5eeff; padding: 16px; border-radius: 8px; }
.tags-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.tags-label { font-size: 12px; color: #434655; font-weight: 600; }
.tags-row { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip { font-size: 14px; background-color: #f5f5f5; color: #434655; padding: 4px 12px; border-radius: 12px; }

.action-btns { margin-top: 20px; display: flex; flex-direction: column; gap: 12px; }
.edit-btn { height: 44px; background-color: #004ac6; color: #ffffff; border-radius: 12px; font-size: 16px; border: none; }
.delete-btn { height: 44px; border: 1px solid #c3c6d7; color: #EF4444; border-radius: 12px; font-size: 16px; background-color: #ffffff; }
</style>
