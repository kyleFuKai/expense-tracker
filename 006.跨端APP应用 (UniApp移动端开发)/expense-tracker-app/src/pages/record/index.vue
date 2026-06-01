<template>
  <view class="record-page">
    <view class="type-tabs">
      <view :class="['type-tab', currentType === 'EXPENSE' ? 'type-tab-active-expense' : '']" @click="currentType = 'EXPENSE'; loadCategories()">支出</view>
      <view :class="['type-tab', currentType === 'INCOME' ? 'type-tab-active-income' : '']" @click="currentType = 'INCOME'; loadCategories()">收入</view>
    </view>

    <view class="amount-row">
      <view class="amount-section">
        <text :class="['amount-label', currentType === 'EXPENSE' ? 'text-expense' : 'text-income']">{{ currentType === 'EXPENSE' ? '支出金额' : '收入金额' }}</text>
        <view class="amount-input-wrap">
          <text class="yen-symbol">¥</text>
          <input class="amount-input" v-model="amountStr" type="digit" placeholder="0.00" />
        </view>
      </view>
      <view class="amount-actions">
        <button class="save-btn" @click="submitBill" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        <view class="time-label" @click="showDatePicker">
          <text>📅</text>
          <text class="time-text">{{ displayDate }}</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-x class="parent-tabs">
      <view v-for="p in parents" :key="p.id" :class="['parent-tab', activeParentId === p.id ? 'parent-tab-active' : '']" @click="selectParent(p.id)">
        <text>{{ p.icon }}</text>
        <text>{{ p.name }}</text>
      </view>
    </scroll-view>

    <view class="category-grid">
      <view v-for="child in currentChildren" :key="child.id" :class="['child-btn', selectedCategoryId === child.id ? 'child-selected' : '']" @click="selectedCategoryId = child.id; selectedCategoryName = child.name">
        <view class="child-icon-wrap"><text>{{ child.icon }}</text></view>
        <text class="child-name">{{ child.name }}</text>
      </view>
      <view v-if="!currentChildren.length" class="no-children"><text>该分类暂无子分类</text></view>
    </view>

    <view class="remark-section">
      <input class="remark-input" v-model="remark" placeholder="添加备注..." />
    </view>

    <view class="tags-section">
      <text class="tags-label">标签</text>
      <scroll-view scroll-x class="tags-scroll">
        <view class="tags-row">
          <view v-for="tag in userTags.slice(0, 12)" :key="tag.id" :class="['tag-chip', selectedTagIds.includes(tag.id) ? 'tag-selected' : '']" @click="toggleTag(tag.id)">{{ tag.name }}</view>
          <view class="tag-chip tag-new" @click="showTagDialog">+ 新建</view>
        </view>
      </scroll-view>
    </view>

    <view v-if="datePickerVisible" class="date-overlay" @click="datePickerVisible = false">
      <view class="date-dialog" @click.stop>
        <text class="date-dialog-title">选择日期</text>
        <picker mode="date" :value="selectedDate" @change="onDateChange">
          <view class="date-picker-btn"><text>{{ selectedDate }}</text></view>
        </picker>
        <view class="date-dialog-btns">
          <button class="date-cancel" @click="datePickerVisible = false">取消</button>
          <button class="date-confirm" @click="confirmDate">确定</button>
        </view>
      </view>
    </view>

    <view v-if="tagDialogVisible" class="date-overlay" @click="tagDialogVisible = false">
      <view class="date-dialog" @click.stop>
        <text class="date-dialog-title">新建标签</text>
        <input class="tag-input" v-model="newTagName" placeholder="输入标签名称（最多 16 个字符）" maxlength="16" />
        <view class="date-dialog-btns">
          <button class="date-cancel" @click="tagDialogVisible = false">取消</button>
          <button class="date-confirm" @click="createNewTag">确认</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCategories } from '../../api/categories'
import { getTags, createTag } from '../../api/tags'
import { createBill, updateBill, getBillById } from '../../api/bills'

const currentType = ref('EXPENSE')
const amountStr = ref('')
const remark = ref('')
const selectedCategoryId = ref(null)
const selectedCategoryName = ref('')
const saving = ref(false)
const editId = ref(null)

const parents = ref([])
const activeParentId = ref(null)

const currentChildren = computed(() => {
  const p = parents.value.find(p => p.id === activeParentId.value)
  return p ? (p.children || []) : []
})

const selectedDate = ref('')
const displayDate = ref('现在')
const datePickerVisible = ref(false)

const userTags = ref([])
const selectedTagIds = ref([])
const tagDialogVisible = ref(false)
const newTagName = ref('')

function loadCategories() {
  getCategories(currentType.value).then(res => {
    if (res.code !== 0) return
    const cats = res.data
    const parentMap = {}
    const childrenMap = {}
    const parentList = []
    const iconMap = {
      restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️',
      movie: '🎬', home: '🏠', medical_services: '💊',
      school: '📚', group: '👥', payments: '💰',
      more_horiz: '📝', default: '📝'
    }
    cats.forEach(cat => {
      const pid = cat.parent_id || 0
      if (pid === 0) {
        parentMap[cat.id] = { id: cat.id, name: cat.name, icon: iconMap[cat.icon] || '📝', children: [] }
        parentList.push(cat.id)
      } else {
        if (!childrenMap[pid]) childrenMap[pid] = []
        childrenMap[pid].push({ id: cat.id, name: cat.name, icon: iconMap[cat.icon] || '📝' })
      }
    })
    parentList.forEach(pid => { parentMap[pid].children = childrenMap[pid] || [] })
    parents.value = parentList.map(pid => parentMap[pid])
    if (parents.value.length) activeParentId.value = parents.value[0].id
  })
}

function selectParent(id) { activeParentId.value = id }

function loadTags() {
  getTags().then(res => {
    if (res.code !== 0) return
    userTags.value = res.data.list || []
  })
}

function toggleTag(id) {
  const idx = selectedTagIds.value.indexOf(id)
  if (idx === -1) selectedTagIds.value.push(id)
  else selectedTagIds.value.splice(idx, 1)
}

function showTagDialog() { newTagName.value = ''; tagDialogVisible.value = true }

function createNewTag() {
  const name = newTagName.value.trim()
  if (!name) { uni.showToast({ title: '请输入标签名称', icon: 'none' }); return }
  if (name.length > 16) { uni.showToast({ title: '标签名不能超过 16 个字符', icon: 'none' }); return }
  createTag({ name }).then(res => {
    if (res.code === 0) {
      userTags.value.push({ id: res.data.id, name })
      selectedTagIds.value.push(res.data.id)
      tagDialogVisible.value = false
    } else {
      uni.showToast({ title: res.msg || '创建失败', icon: 'none' })
    }
  })
}

function showDatePicker() { datePickerVisible.value = true }
function onDateChange(e) { selectedDate.value = e.detail.value }
function confirmDate() {
  if (!selectedDate.value) { datePickerVisible.value = false; return }
  const parts = selectedDate.value.split('-')
  displayDate.value = parts[0] + '/' + parts[1] + '/' + parts[2]
  datePickerVisible.value = false
}

function getNowTime() {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0') + ' ' + String(d.getHours()).padStart(2,'0') + ':' + String(d.getMinutes()).padStart(2,'0') + ':' + String(d.getSeconds()).padStart(2,'0')
}

async function submitBill() {
  const amount = parseFloat(amountStr.value)
  if (!amount || amount <= 0) { uni.showToast({ title: '请输入金额', icon: 'none' }); return }
  if (!selectedCategoryId.value) { uni.showToast({ title: '请选择分类', icon: 'none' }); return }

  saving.value = true
  const billTime = selectedDate.value ? selectedDate.value + ' 00:00:00' : getNowTime()
  const payload = { type: currentType.value, amount, category_id: selectedCategoryId.value, remark: remark.value.trim(), bill_time: billTime, tag_ids: selectedTagIds.value }

  try {
    let res = editId.value ? await updateBill(editId.value, payload) : await createBill(payload)
    if (res.code === 0) {
      uni.switchTab({ url: '/pages/home/index' })
    } else {
      uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
  saving.value = false
}

onLoad((query) => {
  if (query.edit) {
    editId.value = query.edit
    loadBillForEdit(query.edit)
  }
  loadTags()
  loadCategories()
})

async function loadBillForEdit(id) {
  const res = await getBillById(id)
  if (res.code !== 0) return
  const bill = res.data
  amountStr.value = String(parseFloat(bill.amount).toFixed(2))
  remark.value = bill.remark || ''
  if (bill.bill_time) {
    selectedDate.value = bill.bill_time.substring(0, 10)
    const parts = selectedDate.value.split('-')
    displayDate.value = parts[0] + '/' + parts[1] + '/' + parts[2]
  }
  if (bill.tags && bill.tags.length) {
    selectedTagIds.value = bill.tags.map(t => t.id)
  }
  // Wait for categories to load, then select
  setTimeout(() => {
    const targetId = bill.category_id
    for (const p of parents.value) {
      if (p.id === targetId) { selectParent(p.id); selectedCategoryId.value = targetId; selectedCategoryName.value = p.name; return }
      for (const c of (p.children || [])) {
        if (c.id === targetId) { selectParent(p.id); selectedCategoryId.value = targetId; selectedCategoryName.value = c.name; return }
      }
    }
  }, 300)
}
</script>

<style scoped>
.record-page { min-height: 100vh; background-color: #F8FAFC; padding: 16px; }
.type-tabs { display: flex; background-color: #e5eeff; border-radius: 8px; padding: 4px; margin-bottom: 16px; }
.type-tab { flex: 1; text-align: center; padding: 8px; font-size: 14px; border-radius: 6px; color: #434655; }
.type-tab-active-expense { background-color: #ffffff; color: #EF4444; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.type-tab-active-income { background-color: #ffffff; color: #10B981; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }

.amount-row { display: flex; justify-content: space-between; align-items: flex-end; border-bottom: 1px solid #c3c6d7; padding-bottom: 8px; margin-bottom: 16px; }
.amount-label { font-size: 12px; display: block; margin-bottom: 4px; }
.text-expense { color: #EF4444; }
.text-income { color: #10B981; }
.amount-input-wrap { display: flex; align-items: baseline; }
.yen-symbol { font-size: 20px; color: #0b1c30; margin-right: 4px; }
.amount-input { font-size: 28px; font-weight: 700; color: #004ac6; border: none; padding: 0; }
.save-btn { background-color: #004ac6; color: #ffffff; border-radius: 20px; padding: 0 24px; height: 40px; font-size: 14px; font-weight: 700; border: none; }
.time-label { display: flex; align-items: center; gap: 4px; background-color: #dcfce7; color: #006c49; padding: 4px 12px; border-radius: 12px; font-size: 12px; margin-top: 8px; }

.parent-tabs { white-space: nowrap; margin-bottom: 12px; }
.parent-tab { display: inline-flex; align-items: center; gap: 4px; padding: 6px 14px; border-radius: 14px; background-color: #f1f5f9; color: #434655; font-size: 13px; margin-right: 8px; }
.parent-tab-active { background-color: #E8F0FE; color: #004ac6; font-weight: 600; }

.category-grid { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.child-btn { width: 80px; text-align: center; padding: 8px; border-radius: 12px; border: 2px solid transparent; }
.child-selected { border-color: #004ac6; background-color: #E8F0FE; }
.child-icon-wrap { width: 44px; height: 44px; border-radius: 22px; background-color: #f1f5f9; display: flex; align-items: center; justify-content: center; margin: 0 auto 4px; font-size: 22px; }
.child-selected .child-icon-wrap { background-color: #dbeafe; }
.child-name { font-size: 11px; color: #434655; }
.child-selected .child-name { color: #004ac6; font-weight: 600; }
.no-children { width: 100%; text-align: center; color: #94a3b8; padding: 24px; font-size: 13px; }

.remark-section { margin-bottom: 12px; }
.remark-input { height: 44px; background-color: #ffffff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 16px; font-size: 16px; }

.tags-section { margin-bottom: 16px; }
.tags-label { font-size: 12px; color: #434655; display: block; margin-bottom: 8px; }
.tags-scroll { white-space: nowrap; }
.tags-row { display: flex; gap: 8px; }
.tag-chip { display: inline-flex; align-items: center; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; border: 1px solid #ddd; background-color: #f5f5f5; color: #666; white-space: nowrap; }
.tag-selected { background-color: #E8F0FE; color: #004ac6; border-color: #004ac6; }
.tag-new { border-style: dashed; color: #999; }

.date-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0,0,0,0.4); display: flex; align-items: flex-end; justify-content: center; z-index: 100; }
.date-dialog { width: 100%; background-color: #ffffff; border-radius: 16px 16px 0 0; padding: 20px; }
.date-dialog-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; margin-bottom: 16px; }
.date-picker-btn { height: 44px; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 12px; display: flex; align-items: center; }
.tag-input { height: 44px; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 12px; font-size: 16px; margin-bottom: 16px; }
.date-dialog-btns { display: flex; gap: 12px; }
.date-cancel, .date-confirm { flex: 1; height: 44px; border-radius: 12px; font-size: 16px; border: none; }
.date-cancel { border: 1px solid #c3c6d7; color: #434655; }
.date-confirm { background-color: #004ac6; color: #ffffff; }
</style>
