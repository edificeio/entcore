import { odeServices } from '@edifice.io/client';
import { AbsenceSettings, AbsenceSettingsResponse } from '~/models/absence';

/**
 * Creates an absence service with the specified base URL.
 *
 * @param baseURL The base URL for the absence service API.
 * @returns A service to interact with the absence settings.
 */
export const createAbsenceService = (baseURL: string) => ({
  /**
   * Get the current user's absence settings.
   * @returns the settings, or `{}` if none have ever been saved.
   */
  getSettings() {
    return odeServices
      .http()
      .get<Partial<AbsenceSettingsResponse>>(`${baseURL}/absence`);
  },

  /**
   * Upsert the current user's absence settings: covers creation,
   * modification and deactivation alike.
   */
  saveSettings(payload: AbsenceSettings) {
    return odeServices
      .http()
      .put<AbsenceSettingsResponse>(`${baseURL}/absence`, payload);
  },
});
