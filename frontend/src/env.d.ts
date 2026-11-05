interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_DEV_USER_ID?: string
  readonly VITE_DEV_ROLE?: string
  readonly VITE_DEV_USER_NAME?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
