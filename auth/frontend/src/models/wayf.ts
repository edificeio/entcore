export interface WayfProvider {
  i18n: string;
  acs?: string;
  children?: WayfProvider[];
  color?: string;
  iconSrc?: string;
}

export interface WayfDomainConfig {
  providers: WayfProvider[];
}

export interface WayfConfig {
  'wayf-v2': Record<string, WayfDomainConfig>;
}
