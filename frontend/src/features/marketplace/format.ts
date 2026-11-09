import dayjs from 'dayjs'

/** Ngày giờ theo giờ địa phương, dạng dd/MM/yyyy HH:mm. Chuỗi rỗng thì để trống. */
export function formatDateTime(value?: string): string {
  if (!value) {
    return ''
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY HH:mm') : ''
}

/** Chỉ ngày, dạng dd/MM/yyyy. */
export function formatDate(value?: string): string {
  if (!value) {
    return ''
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('DD/MM/YYYY') : ''
}

/** Giá VND đọc được; 0 hiển thị "Miễn phí" cho khớp cách MVP chỉ có khóa free. */
export function formatPrice(price: string, currency = 'VND'): string {
  const value = Number(price)
  if (!Number.isFinite(value) || value === 0) {
    return 'Miễn phí'
  }
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(value)
}
