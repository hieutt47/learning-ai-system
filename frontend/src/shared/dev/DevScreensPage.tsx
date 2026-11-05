import { Card, Segmented, Space, Table, Tag, Typography } from 'antd'
import { Link } from 'react-router'
import { ROLE_LABEL, setDevRoleOverride, useCurrentUser, type ViewerRole } from '../auth/currentUser'
import { OWNER_LABEL, SCREENS, type Access, type Owner, type Screen } from '../routing/screens'

/**
 * CHỈ KHI DEV (/dev/screens): mở nhanh mọi màn hình trong lúc chưa có nút điều hướng,
 * và đổi vai trò xem giao diện mà không phải sửa .env.local. Bản build không có trang này.
 */

const ACCESS_LABEL: Record<Access, string> = {
  public: 'Ai cũng xem',
  guest: 'Chỉ khách',
  auth: 'Đã đăng nhập',
  STUDENT: 'Sinh viên',
  TEACHER: 'Giảng viên',
  ADMIN: 'Admin',
}

function canOpen(access: Access, role: ViewerRole): boolean {
  if (access === 'public') return true
  if (access === 'guest') return role === 'GUEST'
  if (access === 'auth') return role !== 'GUEST'
  return access === role
}

const ROLE_OPTIONS = [
  { label: 'Khách', value: 'GUEST' },
  { label: ROLE_LABEL.STUDENT, value: 'STUDENT' },
  { label: ROLE_LABEL.TEACHER, value: 'TEACHER' },
  { label: ROLE_LABEL.ADMIN, value: 'ADMIN' },
]

export function DevScreensPage() {
  const { role } = useCurrentUser()
  const owners = [1, 2, 3, 4, 5] as Owner[]

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <Card>
        <Space direction="vertical">
          <Typography.Text strong>Vai trò đang xem giao diện</Typography.Text>
          <Space wrap>
            <Segmented
              options={ROLE_OPTIONS}
              value={role}
              onChange={(value) => setDevRoleOverride(value as ViewerRole)}
            />
            <Typography.Link onClick={() => setDevRoleOverride(null)}>Theo .env.local</Typography.Link>
          </Space>
          <Typography.Text type="secondary">
            Chỉ đổi menu và quyền vào trang phía giao diện. Header X-User-Id gửi lên backend vẫn lấy từ
            VITE_DEV_USER_ID trong .env.local.
          </Typography.Text>
        </Space>
      </Card>

      {owners.map((owner) => {
        const rows = Object.entries(SCREENS)
          .filter(([, s]) => s.owner === owner)
          .map(([key, s]) => ({ key, ...(s as Screen) }))
        return (
          <Card key={owner} title={OWNER_LABEL[owner]}>
            <Table
              size="small"
              pagination={false}
              dataSource={rows}
              columns={[
                {
                  title: 'Màn hình',
                  dataIndex: 'title',
                  render: (title: string, s: Screen) => <Link to={s.sample}>{title}</Link>,
                },
                { title: 'Đường dẫn mẫu', dataIndex: 'sample', render: (v: string) => <code>{v}</code> },
                { title: 'Prototype', dataIndex: 'prototype', render: (v: string) => <code>#{v}</code> },
                {
                  title: 'Quyền',
                  dataIndex: 'access',
                  render: (access: Access) => (
                    <Tag color={canOpen(access, role) ? 'success' : 'default'}>{ACCESS_LABEL[access]}</Tag>
                  ),
                },
              ]}
            />
          </Card>
        )
      })}
    </Space>
  )
}
