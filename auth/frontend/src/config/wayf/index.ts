import type { WayfConfig } from '~/models/wayf';
import { DEFAULT_WAYF_CONFIG } from './default';
import { hdfConfig, natiConfig } from './domains';

export { DEFAULT_WAYF_CONFIG } from './default';

/**
 * Per-domain WAYF configurations, indexed by hostname.
 *
 * To add a domain: create a file in `./domains/<name>.ts` exporting a
 * `WayfDomainConfig`, import it here, and map its hostname(s) below.
 * Any hostname not listed here falls back to `DEFAULT_WAYF_CONFIG`.
 */
export const wayfConfig: WayfConfig = {
  'wayf-v2': {
    'connexion.enthdf.fr': hdfConfig,
    'nati.pf': natiConfig,
    'localhost': DEFAULT_WAYF_CONFIG,
  },
};
