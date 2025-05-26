import { odeServices } from '@edifice.io/client';

export const createMyAppsPreferencesService = () => ({
  async getMyAppsPreferences() {
    const res = await odeServices.http().get<{ preference: string }>('/userbook/preference/my-apps');
    return JSON.parse(res.preference);
  },

  async updateMyAppsPreferences(preferences: { tab: string }) {
    return odeServices.http().put('/userbook/preference/my-apps', preferences);
  },
});
