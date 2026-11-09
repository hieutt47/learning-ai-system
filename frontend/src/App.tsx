import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp } from 'antd'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { useEffect } from 'react'
import { RouterProvider } from 'react-router'
import { queryClient } from './shared/api/queryClient'
import { restoreSession } from './shared/auth/session'
import { router } from './shared/routing/router'
import { ThemeModeProvider } from './shared/theme/ThemeModeProvider'

dayjs.locale('vi')

export default function App() {
  useEffect(() => {
    void restoreSession()
  }, [])

  return (
    <ThemeModeProvider>
      {/* AntApp cung cấp message/notification/modal theo theme: dùng App.useApp() trong trang */}
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </AntApp>
    </ThemeModeProvider>
  )
}
