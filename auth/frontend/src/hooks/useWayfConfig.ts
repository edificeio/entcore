import { DEFAULT_WAYF_CONFIG, wayfConfig } from '~/config/wayf.config';
import type { WayfDomainConfig } from '~/models/wayf';

export function useWayfConfig(): WayfDomainConfig {
  const hostname = window.location.hostname;
  return wayfConfig['wayf-v2'][hostname] ?? DEFAULT_WAYF_CONFIG;
}
