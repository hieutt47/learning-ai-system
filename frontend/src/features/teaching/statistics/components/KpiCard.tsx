import type { ReactNode } from 'react'

export type KpiTone = 'primary' | 'success' | 'warning' | 'info'

interface KpiCardProps {
  label: string
  value: string | number
  suffix?: string
  sub?: string
  icon: ReactNode
  tone?: KpiTone
}

export function KpiCard({ label, value, suffix, sub, icon, tone = 'primary' }: KpiCardProps) {
  return (
    <div className="stats-kpi-card">
      <div className="stats-kpi-head">
        <span className="stats-kpi-label">{label}</span>
        <span className={`stats-kpi-icon tone-${tone}`} aria-hidden="true">
          {icon}
        </span>
      </div>
      <div className="stats-kpi-value">
        {value}
        {suffix ? <small> {suffix}</small> : null}
      </div>
      {sub ? <span className="stats-kpi-sub">{sub}</span> : null}
    </div>
  )
}
