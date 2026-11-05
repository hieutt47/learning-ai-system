import type { Params } from 'react-router'

export interface TabItem {
  to: string
  label: string
}

/** Dữ liệu gắn vào route (thuộc tính `handle`), AppLayout đọc để dựng header và thanh tab. */
export interface RouteHandle {
  /** Tiêu đề trang trên header (route sâu nhất có title thắng). */
  title?: string
  /** Hiện ô "Tìm khóa học…" trên header (marketplace, khóa học của tôi). */
  search?: boolean
  /** Thanh tab giữa header và nội dung, như tabsBar() của prototype. Mỗi tab là một route con. */
  tabs?: (params: Params) => TabItem[]
}
