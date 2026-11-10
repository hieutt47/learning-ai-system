import dayjs from 'dayjs'

export function formatDateTime(value?: string): string {
  if (!value) {
    return ''
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY HH:mm') : ''
}

export function formatDate(value?: string): string {
  if (!value) {
    return ''
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY') : ''
}

export function formatPrice(price: string, currency = 'VND'): string {
  const value = Number(price)
  if (!Number.isFinite(value) || value === 0) {
    return 'Miễn phí'
  }
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(value)
}
