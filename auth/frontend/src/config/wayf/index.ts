import type { WayfConfig } from '~/models/wayf';
import { DEFAULT_WAYF_CONFIG } from './default';
import { hdfConfig } from './domains/hdf';

export { DEFAULT_WAYF_CONFIG } from './default';
// Re-exported so the config is ready to map to its hostname when its domain
// goes live (not active yet — kept here to avoid losing the config).
export { reunionConfig } from './domains/reunion';

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
    'localhost': DEFAULT_WAYF_CONFIG,
  },
};
