import { useQuery } from '@tanstack/react-query'
import { Card, Tag } from 'antd'
import { ROLE_LABEL } from '../../shared/auth/currentUser'
import { updateCurrentUser } from '../../shared/auth/session'
import { Avatar } from '../../shared/components/AppLayout'
import { ErrorState, LoadingState } from '../../shared/components/PageState'
import '../../shared/styles/management.css'
import { TeacherRequestSection } from '../teacher-requests/TeacherRequestSection'
import { getProfile } from './api'
import { ProfileForm } from './ProfileForm'

export function ProfilePage() {
  const query = useQuery({
    queryKey: ['users', 'me'],
    queryFn: async ({ signal }) => {
      const user = await getProfile(signal)
      signal.throwIfAborted()
      updateCurrentUser(user)
      return user
    },
    staleTime: 0,
  })

  if (query.isPending) return <LoadingState />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const user = query.data

  return (
    <div className="account-page account-profile">
      <p className="account-description">Quản lý thông tin tài khoản và theo dõi yêu cầu trở thành giảng viên.</p>
      <Card>
        <div className="account-identity">
          <Avatar name={user.full_name} size="xl" />
          <div className="account-identity-info"><h2>{user.full_name}</h2><p>{user.email}</p></div>
          <Tag color="processing">{ROLE_LABEL[user.role]}</Tag>
        </div>
      </Card>
      <ProfileForm user={user} />
      <TeacherRequestSection user={user} onRefreshUser={() => void query.refetch()} />
    </div>
  )
}
