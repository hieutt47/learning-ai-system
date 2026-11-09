import dayjs from 'dayjs'
import type { IsoDateTime } from '../api/types'

export function formatDateTime(value: IsoDateTime | null): string {
  return value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '—'
}
