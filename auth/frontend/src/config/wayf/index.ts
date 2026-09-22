import type { WayfConfig } from '~/models/wayf';
import { DEFAULT_WAYF_CONFIG } from './default';
import {
  ent04Config,
  guadeloupeConfig,
  hdfConfig,
  leiaConfig,
  natiConfig,
  normandieConfig,
  primotConfig,
  semConfig,
  vcaConfig,
} from './domains';

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
    'ent.leia.corsica': leiaConfig,
    'ent04.fr': ent04Config,
    'enthdf.fr': hdfConfig,
    'karukera.ac-guadeloupe.fr': guadeloupeConfig,
    'nati.pf': natiConfig,
    'ent.l-educdenormandie.fr': normandieConfig,
    'www.primot.fr': primotConfig,
    'sem.edifice.io': semConfig,
    'ent.vienne-condrieu-agglomeration.fr': vcaConfig,
    'localhost': DEFAULT_WAYF_CONFIG,
  },
};
