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

interface WayfBaseProvider {
  i18n: string;
  /** CSS modifier key — must match a `.wayf-provider-btn--{color}` class */
  color: string;
  icon?: WayfIconKey;
  iconSrc?: string;
}

export interface WayfLeafProvider extends WayfBaseProvider {
  acs: string;
  children?: never;
}

export interface WayfParentProvider extends WayfBaseProvider {
  children: WayfProvider[];
  acs?: never;
}

export type WayfProvider = WayfLeafProvider | WayfParentProvider;

export interface WayfPartner {
  /** Logo asset URL (e.g. `/assets/themes/.../logo.png`) */
  logo: string;
  /** Optional link wrapping the logo */
  url?: string;
}

export interface WayfDomainConfig {
  providers: WayfProvider[];
  partners?: WayfPartner[];
}

export interface WayfConfig {
  'wayf-v2': Record<string, WayfDomainConfig>;
}
