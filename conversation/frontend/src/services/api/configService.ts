import { APP } from '@edifice.io/client';
import { odeServices } from 'edifice-ts-client';

type SignaturePreferences = {
  useSignature?: boolean;
  signature?: string;
};

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
  setSignaturePreferences(value: SignaturePreferences) {
    return odeServices
      .conf()
      .savePreference(APP.CONVERSATION, JSON.stringify(value));
  },
});
