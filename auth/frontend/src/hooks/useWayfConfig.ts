import { DEFAULT_WAYF_CONFIG, wayfConfig } from '~/config/wayf';
import type { WayfDomainConfig } from '~/models/wayf';

/**
 * Cookie used to override the resolved domain. Lets QA preview any domain's
 * WAYF config without changing the actual hostname — set it from the browser
 * devtools, e.g. `document.cookie = 'wayf-domain=connexion.enthdf.fr'`.
 */
export const WAYF_DOMAIN_COOKIE = 'wayf-domain';

function getDomainOverride(): string | undefined {
  const match = document.cookie.match(
    new RegExp(`(?:^|;\\s*)${WAYF_DOMAIN_COOKIE}=([^;]+)`),
  );
  return match ? decodeURIComponent(match[1]) : undefined;
}

/**
 * Resolves the WAYF config for the current domain.
 *
 * The domain is taken from the `wayf-domain` cookie when present (QA override),
 * otherwise from `window.location.hostname`. Falls back to
 * `DEFAULT_WAYF_CONFIG` when the resolved domain has no dedicated config.
 */
export function useWayfConfig(): WayfDomainConfig {
  const hostname = getDomainOverride() ?? window.location.hostname;
  return wayfConfig['wayf-v2'][hostname] ?? DEFAULT_WAYF_CONFIG;
}
