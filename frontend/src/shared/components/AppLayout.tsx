import { App, Dropdown, type MenuProps } from 'antd'
import { useState, type KeyboardEvent } from 'react'
import { Link, Outlet, useLocation, useMatches, useNavigate } from 'react-router'
import { ROLE_LABEL, useCurrentUser, type ViewerRole } from '../auth/currentUser'
import type { RouteHandle, TabItem } from '../routing/routeHandle'
import { paths } from '../routing/paths'
import { useThemeMode } from '../theme/themeMode'
import { Icon } from './Icon'
import { PageHeaderContext, type PageHeader } from './pageHeader'
import type { IconName } from './iconPaths'

/**
 * Khung app theo docs/03-experience/prototype.html (hàm nav() và shell()): sidebar 248px có logo,
 * nhóm menu theo vai trò, ô tài khoản (hoặc ô đăng nhập cho khách) ở đáy; header có tiêu đề trang,
 * ô tìm khóa học (trang có handle.search) và menu tài khoản. CSS ở styles/shell.css, giữ tên class prototype.
 */

interface NavItem {
  to: string
  icon: IconName
  label: string
  /** Tiền tố đường dẫn làm mục này sáng lên (vd trang con của khóa học). */
  match: string[]
}

interface NavGroup {
  heading: string
  items: NavItem[]
}

const ACCOUNT_GROUP: NavGroup = {
  heading: 'Tài khoản',
  items: [{ to: paths.profile, icon: 'user', label: 'Hồ sơ', match: [paths.profile] }],
}

const EXPLORE: NavItem = { to: paths.explore, icon: 'compass', label: 'Khám phá khóa học', match: [paths.explore, '/courses'] }

const NAV_BY_ROLE: Record<ViewerRole, NavGroup[]> = {
  GUEST: [{ heading: 'Học tập', items: [EXPLORE] }],
  STUDENT: [
    {
      heading: 'Học tập',
      items: [
        EXPLORE,
        { to: paths.myCourses, icon: 'book', label: 'Khóa học của tôi', match: [paths.myCourses, '/learn', '/quizzes', '/results'] },
        { to: paths.attempts, icon: 'clock', label: 'Lịch sử làm bài', match: [paths.attempts] },
      ],
    },
    ACCOUNT_GROUP,
  ],
  TEACHER: [
    {
      heading: 'Giảng dạy',
      items: [
        {
          to: paths.teachingCourses,
          icon: 'folder',
          label: 'Khóa học giảng dạy',
          match: [paths.teachingCourses, '/teaching/lessons', '/teaching/quizzes'],
        },
        { to: paths.teachingStatistics, icon: 'chart', label: 'Thống kê', match: [paths.teachingStatistics] },
      ],
    },
    ACCOUNT_GROUP,
  ],
  ADMIN: [
    {
      heading: 'Quản trị',
      items: [
        { to: paths.adminUsers, icon: 'users', label: 'Người dùng', match: [paths.adminUsers] },
        { to: paths.adminTeacherRequests, icon: 'checkc', label: 'Duyệt giảng viên', match: [paths.adminTeacherRequests] },
        { to: paths.adminCategories, icon: 'tag', label: 'Danh mục', match: [paths.adminCategories] },
        { to: paths.adminCourses, icon: 'folder', label: 'Khóa học', match: [paths.adminCourses] },
        { to: paths.adminStatistics, icon: 'chart', label: 'Thống kê hệ thống', match: [paths.adminStatistics] },
      ],
    },
    ACCOUNT_GROUP,
  ],
}

function isActive(pathname: string, prefixes: string[]): boolean {
  return prefixes.some((p) => pathname === p || pathname.startsWith(`${p}/`))
}

/** Chữ viết tắt và màu avatar giống prototype (ini, tone). */
const TONES = ['primary', 'success', 'warning', 'neutral'] as const
function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
}
function tone(name: string): string {
  return TONES[[...name].reduce((sum, c) => sum + c.charCodeAt(0), 0) % TONES.length]
}

export function Avatar({ name, size }: { name: string; size?: 'sm' | 'lg' | 'xl' }) {
  return <span className={`avatar ${size ?? ''} tone-${tone(name)}`}>{initials(name)}</span>
}

function Sidebar() {
  const { role, name } = useCurrentUser()
  const { pathname } = useLocation()

  return (
    <nav className="sb" aria-label="Điều hướng chính">
      <Link className="logo" to={paths.home}>
        <span className="logo-mk">
          <Icon name="cap" size={18} />
        </span>
        <b>Learning AI</b>
      </Link>

      {NAV_BY_ROLE[role].map((group) => (
        <div className="nav-g" key={group.heading}>
          <span className="nav-h">{group.heading}</span>
          {group.items.map((item) => {
            const on = isActive(pathname, item.match)
            return (
              <Link key={item.to} className={`nav-i ${on ? 'on' : ''}`} to={item.to} aria-current={on ? 'page' : undefined}>
                <Icon name={item.icon} />
                {item.label}
              </Link>
            )
          })}
        </div>
      ))}

      {import.meta.env.DEV && (
        <div className="nav-g">
          <span className="nav-h">Dev</span>
          <Link className={`nav-i ${pathname === '/dev/screens' ? 'on' : ''}`} to="/dev/screens">
            <Icon name="list" />
            Danh sách màn hình
          </Link>
        </div>
      )}

      {role === 'GUEST' || !name ? (
        <div className="gbox">
          <p>Đăng nhập để ghi danh, làm quiz và lưu tiến độ học. Hoàn toàn miễn phí.</p>
          <Link className="btn btn-primary btn-block" to={paths.login}>
            Đăng nhập
          </Link>
          <Link className="btn btn-secondary btn-block" to={paths.register}>
            Đăng ký
          </Link>
        </div>
      ) : (
        <div className="ubox">
          <Avatar name={name} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <b>{name}</b>
            <span className="st">{ROLE_LABEL[role]}</span>
          </div>
        </div>
      )}
    </nav>
  )
}

function HeaderSearch() {
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      navigate(q.trim() ? `${paths.explore}?q=${encodeURIComponent(q.trim())}` : paths.explore)
    }
  }
  return (
    <label className="search">
      <Icon name="search" />
      <input value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={onKeyDown} placeholder="Tìm khóa học…" aria-label="Tìm khóa học" />
    </label>
  )
}

function AccountMenu({ name, role }: { name: string; role: ViewerRole }) {
  const navigate = useNavigate()
  const { toggle } = useThemeMode()
  const { message } = App.useApp()

  const items: MenuProps['items'] = [
    {
      key: 'who',
      disabled: true,
      label: (
        <div style={{ padding: '2px 0' }}>
          <b style={{ color: 'var(--text)' }}>{name}</b>
          <div className="st">{role !== 'GUEST' ? ROLE_LABEL[role] : ''}</div>
        </div>
      ),
    },
    { type: 'divider' },
    { key: 'profile', icon: <Icon name="user" />, label: 'Hồ sơ', onClick: () => navigate(paths.profile) },
    { key: 'theme', icon: <Icon name="moon" />, label: 'Đổi giao diện sáng / tối', onClick: toggle },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <Icon name="out" />,
      label: 'Đăng xuất',
      // TODO(stub): gọi POST /auth/logout khi phần auth merge.
      onClick: () => void message.info('Chưa có đăng nhập thật (auth chưa merge).'),
    },
  ]

  return (
    <Dropdown menu={{ items }} trigger={['click']} placement="bottomRight">
      <button type="button" className="ava-btn" aria-label="Menu tài khoản">
        <Avatar name={name} />
        <Icon name="down" />
      </button>
    </Dropdown>
  )
}

/** Thanh tab giữa header và nội dung (tabsBar của prototype). Tab sáng: tab có đường dẫn dài nhất khớp URL. */
function TabBar({ items }: { items: TabItem[] }) {
  const { pathname } = useLocation()
  const active = items
    .map((t) => t.to)
    .filter((to) => pathname === to || pathname.startsWith(`${to}/`))
    .sort((a, b) => b.length - a.length)[0]
  return (
    <nav className="tabs" aria-label="Phần của trang">
      {items.map((t) => (
        <Link key={t.to} className={`tab ${t.to === active ? 'on' : ''}`} to={t.to} aria-current={t.to === active ? 'page' : undefined}>
          {t.label}
        </Link>
      ))}
    </nav>
  )
}

function Breadcrumb({ crumbs }: { crumbs: NonNullable<PageHeader['crumbs']> }) {
  return (
    <ol className="crumb" aria-label="Breadcrumb">
      {crumbs.map((c, i) => {
        const last = i === crumbs.length - 1
        return (
          <li key={`${c.label}-${i}`} style={{ display: 'contents' }}>
            {last || !c.to ? <span aria-current={last ? 'page' : undefined}>{c.label}</span> : <Link to={c.to}>{c.label}</Link>}
            {!last && <span aria-hidden="true">/</span>}
          </li>
        )
      })}
    </ol>
  )
}

export function AppLayout() {
  const { role, name } = useCurrentUser()
  const matches = useMatches()
  const [pageHeader, setPageHeader] = useState<PageHeader | null>(null)

  const deepest = <K extends keyof RouteHandle>(key: K) =>
    [...matches].reverse().find((m) => (m.handle as RouteHandle | undefined)?.[key] !== undefined)
  const titleMatch = deepest('title')
  const tabsMatch = deepest('tabs')
  const handle = titleMatch?.handle as RouteHandle | undefined
  const tabs = tabsMatch ? (tabsMatch.handle as RouteHandle).tabs?.(tabsMatch.params) : undefined
  const title = pageHeader?.title ?? handle?.title ?? 'Learning AI'
  const crumbs = pageHeader?.crumbs

  return (
    <PageHeaderContext value={setPageHeader}>
      <div className="shell">
        <Sidebar />
        <div className="main">
          <header className="hdr">
            <div className="grow">
              {crumbs && crumbs.length > 0 && <Breadcrumb crumbs={crumbs} />}
              <h1>{title}</h1>
            </div>
            {handle?.search && <HeaderSearch />}
            {name && <AccountMenu name={name} role={role} />}
          </header>
          {tabs && <TabBar items={tabs} />}
          <main className="content">
            <Outlet />
          </main>
        </div>
      </div>
    </PageHeaderContext>
  )
}
