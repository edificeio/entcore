import { odeServices } from 'edifice-ts-client';

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
});
