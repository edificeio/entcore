import { DEFAULT_WAYF_CONFIG } from '~/config/wayf/default';
import type { WayfDomainConfig } from '~/models/wayf';

export const eprimoConfig: WayfDomainConfig = {
  ...DEFAULT_WAYF_CONFIG,
  edificeLogoClickable: false,
};
