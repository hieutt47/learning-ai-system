import { Card, Space, Table, Tag, Typography } from 'antd'
import { Link } from 'react-router'
import { ROLE_LABEL, useCurrentUser, type ViewerRole } from '../auth/currentUser'
import { OWNER_LABEL, SCREENS, type Access, type Owner, type Screen } from '../routing/screens'

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

export function DevScreensPage() {
  const { role, name, isLoading } = useCurrentUser()
  const owners = [1, 2, 3, 4, 5] as Owner[]

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <Card>
        <Space direction="vertical">
          <Typography.Text strong>Tài khoản hiện tại</Typography.Text>
          <Typography.Text>
            {isLoading ? 'Đang kiểm tra phiên đăng nhập…' : role === 'GUEST' ? 'Khách' : `${name} · ${ROLE_LABEL[role]}`}
          </Typography.Text>
          <Typography.Text type="secondary">
            Menu và quyền truy cập dựa trên tài khoản đăng nhập thật. Đăng nhập bằng tài khoản có vai trò phù hợp để kiểm tra từng màn hình.
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
