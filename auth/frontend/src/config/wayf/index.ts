import type { WayfConfig } from '~/models/wayf';
import { DEFAULT_WAYF_CONFIG } from './default';
import {
  cd13Config,
  ent04Config,
  ent05Config,
  guadeloupeConfig,
  guyaneConfig,
  hdfConfig,
  leiaConfig,
  moselleConfig,
  natiConfig,
  normandieConfig,
  primotConfig,
  semConfig,
  varConfig,
  vcaConfig,
  vosgesConfig,
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
    'www.eduprovence.fr': cd13Config,
    'ent04.fr': ent04Config,
    'ent.colleges05.fr': ent05Config,
    'enthdf.fr': hdfConfig,
    'karukera.ac-guadeloupe.fr': guadeloupeConfig,
    'wilapa-guyane.com': guyaneConfig,
    'ariane57.moselle-education.fr': moselleConfig,
    'nati.pf': natiConfig,
    'ent.l-educdenormandie.fr': normandieConfig,
    'www.primot.fr': primotConfig,
    'sem.edifice.io': semConfig,
    'moncollege-ent.var.fr': varConfig,
    'ent.vienne-condrieu-agglomeration.fr': vcaConfig,
    'panda.vosges.fr': vosgesConfig,
    'localhost': DEFAULT_WAYF_CONFIG,
  },
};
