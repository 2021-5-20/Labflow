export function formatDateTime(value?: string | null) {
  if (!value) return '未记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const currentYear = new Date().getFullYear()
  const showYear = date.getFullYear() !== currentYear
  return new Intl.DateTimeFormat('zh-CN', {
    ...(showYear ? { year: 'numeric' as const } : {}),
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date)
}

export function formatRelativeTime(value?: string | null) {
  if (!value) return '未记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const diffSeconds = Math.round((date.getTime() - Date.now()) / 1000)
  const absSeconds = Math.abs(diffSeconds)
  const divisions = [
    { amount: 60, unit: 'second' },
    { amount: 60, unit: 'minute' },
    { amount: 24, unit: 'hour' },
    { amount: 7, unit: 'day' },
    { amount: 4.345, unit: 'week' },
    { amount: 12, unit: 'month' },
    { amount: Number.POSITIVE_INFINITY, unit: 'year' }
  ] as const

  let duration = diffSeconds
  let absDuration = absSeconds
  for (const division of divisions) {
    if (absDuration < division.amount) {
      return new Intl.RelativeTimeFormat('zh-CN', { numeric: 'auto' }).format(Math.round(duration), division.unit)
    }
    duration /= division.amount
    absDuration /= division.amount
  }
  return ''
}
