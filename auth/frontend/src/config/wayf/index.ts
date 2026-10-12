import type { WayfConfig } from '~/models/wayf';
import { DEFAULT_WAYF_CONFIG } from './default';
import {
  cd13Config,
  cd84Config,
  ent04Config,
  ent05Config,
  eprimoConfig,
  guadeloupeConfig,
  guyaneConfig,
  hdfConfig,
  leiaConfig,
  martiniqueConfig,
  mayotteConfig,
  moncollegeConfig,
  monecoleConfig,
  moselleConfig,
  natiConfig,
  normandieConfig,
  portovecchioConfig,
  primotConfig,
  reimsConfig,
  reunionConfig,
  semConfig,
  toulonConfig,
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
    'www.aucollege84.vaucluse.fr': cd84Config,
    'ent04.fr': ent04Config,
    'ent.colleges05.fr': ent05Config,
    'ent.e-primo.fr': eprimoConfig,
    'enthdf.fr': hdfConfig,
    'karukera.ac-guadeloupe.fr': guadeloupeConfig,
    'wilapa-guyane.com': guyaneConfig,
    'colibri.ac-martinique.fr': martiniqueConfig,
    'mayotte.edifice.io': mayotteConfig,
    'www.moncollege-ent.essonne.fr': moncollegeConfig,
    'monecole-ent.essonne.fr': monecoleConfig,
    'ariane57.moselle-education.fr': moselleConfig,
    'nati.pf': natiConfig,
    'ent.l-educdenormandie.fr': normandieConfig,
    'portivechju.edifice.io': portovecchioConfig,
    'www.primot.fr': primotConfig,
    'ent-ecoles.ac-reims.fr': reimsConfig,
    'ent1d.ac-reunion.fr': reunionConfig,
    'sem.edifice.io': semConfig,
    'ent.toulon.fr': toulonConfig,
    'moncollege-ent.var.fr': varConfig,
    'ent.vienne-condrieu-agglomeration.fr': vcaConfig,
    'panda.vosges.fr': vosgesConfig,
    'localhost': DEFAULT_WAYF_CONFIG,
  },
};
