import { odeServices } from '@edifice.io/client';

export const createPreferencesService = () => ({
  async getUserPreferences() {
    const res = await odeServices.http().get<{ preference: string }>('/userbook/preference/apps');
    return JSON.parse(res.preference);
  },

  async updateUserPreferences(preferences: { bookmarks: string[]; applications: string[] }) {
    return odeServices.http().put('/userbook/preference/apps', preferences);
  },
});