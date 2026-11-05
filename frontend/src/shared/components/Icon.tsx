import { ICON_PATHS, type IconName } from './iconPaths'

/** Icon nét theo prototype. Màu theo chữ xung quanh (currentColor); size mặc định 16px. */
export function Icon({ name, size, className }: { name: IconName; size?: number; className?: string }) {
  return (
    <svg
      className={className ? `ic ${className}` : 'ic'}
      style={size ? { width: size, height: size } : undefined}
      viewBox="0 0 24 24"
      aria-hidden="true"
      // Nội dung lấy từ bảng hằng số ICON_PATHS trong code, không phải dữ liệu người dùng.
      dangerouslySetInnerHTML={{ __html: ICON_PATHS[name] }}
    />
  )
}
