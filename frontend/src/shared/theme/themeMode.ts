import { createContext, useContext } from 'react'
import type { ThemeMode } from './antdTheme'

export interface ThemeModeValue {
  mode: ThemeMode
  toggle: () => void
}

export const ThemeModeContext = createContext<ThemeModeValue>({ mode: 'light', toggle: () => {} })

/** Chế độ sáng/tối hiện tại và hàm đổi (menu tài khoản: "Đổi giao diện sáng / tối"). */
export function useThemeMode(): ThemeModeValue {
  return useContext(ThemeModeContext)
}

const STORAGE_KEY = 'las.theme'

export function initialThemeMode(): ThemeMode {
  try {
    const saved = window.localStorage.getItem(STORAGE_KEY)
    if (saved === 'light' || saved === 'dark') {
      return saved
    }
  } catch {
    // localStorage bị chặn (chế độ riêng tư...): theo hệ thống.
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function saveThemeMode(mode: ThemeMode): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    // Không lưu được thì thôi, lần sau theo hệ thống.
  }
}
