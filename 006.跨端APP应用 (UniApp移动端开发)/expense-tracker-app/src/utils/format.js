export function fmtMoney(n) {
  return '¥' + parseFloat(n).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

export function escapeHtml(str) {
  if (str == null) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
}

export function fmtDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10).replace(/-/g, '/')
}

export function getDayOfWeek(dateStr) {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const d = new Date(dateStr)
  return days[d.getDay()]
}

export function getCurrentMonth() {
  const now = new Date()
  return now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0')
}
