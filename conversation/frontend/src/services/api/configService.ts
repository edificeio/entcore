import { APP } from '@edifice.io/client';
import { odeServices } from '@edifice.io/client';
import { SignaturePreferences } from '~/models/signature';

/**
 * Creates a configuration service with the specified base URL.
 *
 * @param baseURL The base URL for the configuration service API.
 * @returns A service to interact with configuration.
 */
export const createConfigService = (baseURL: string) => ({
  /**
   * Get configuration.
   * @returns a Configuration object
   */
  getGlobalConfig() {
    return odeServices.http().get<{
      'max-depth': number;
      'recall-delay-minutes': number;
      'get-visible-strategy': 'all-at-once' | 'filtered';
    }>(`${baseURL}/max-depth`);
  },

  /**
   * Get signature and its preferences.
   */
  getSignaturePreferences() {
    return odeServices
      .conf()
      .getPreference<SignaturePreferences>(APP.CONVERSATION);
  },

  /**
   * Set signature preferences.
   */
  setSignaturePreferences(preferences: SignaturePreferences) {
    return odeServices.conf().savePreference(APP.CONVERSATION, preferences);
  },
});
