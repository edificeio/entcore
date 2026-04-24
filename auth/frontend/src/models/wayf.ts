/**
 * Finite list of built-in icon keys available to providers.
 * Extend this const to add more icons (then register them in ProviderButton).
 */
export const WAYF_ICONS = [
  'student',
  'teacher',
  'relative',
  'perseducnat',
  'other',
] as const;

export type WayfIconKey = (typeof WAYF_ICONS)[number];

export interface WayfProvider {
  i18n: string;
  acs?: string;
  children?: WayfProvider[];
  color?: string;
  iconSrc?: string;
  /** Explicit icon from the built-in finite list. Decoupled from the i18n key. */
  icon?: WayfIconKey;
  /** Overrides the default level-2 title when this provider is expanded */
  titleI18n?: string;
}

export interface WayfDomainConfig {
  providers: WayfProvider[];
}

export interface WayfConfig {
  'wayf-v2': Record<string, WayfDomainConfig>;
}
