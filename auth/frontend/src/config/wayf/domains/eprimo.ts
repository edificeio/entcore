import { DEFAULT_WAYF_CONFIG } from '~/config/wayf/default';
import type { WayfDomainConfig } from '~/models/wayf';

// E-Primo does not want the Édifice logo to link to edifice.io (ENABLING-1208).
// Not yet wired to a hostname in `../index.ts` — E-Primo's production domain
// hasn't been added to the WAYF v2 rollout yet.
export const eprimoConfig: WayfDomainConfig = {
  ...DEFAULT_WAYF_CONFIG,
  edificeLogoClickable: false,
};
