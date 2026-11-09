import { Button, type ButtonProps } from 'antd'

export function AiButton({ style, ...props }: ButtonProps) {
  return (
    <Button
      {...props}
      style={{
        background: 'var(--ai)',
        borderColor: 'var(--ai)',
        color: 'var(--surface)',
        ...style,
      }}
    />
  )
}
