import { odeServices } from '@edifice.io/client';

type UserPrefs = {
  homePage: { betaEnabled: boolean } | null;
  background: string | null;
};

export const preferenceService = {
  deactivateHomepage: () => {
    return odeServices.http().put<UserPrefs>('/userbook/api/preferences', {
      homePage: { betaEnabled: false },
    });
  },

  saveCustomization: (language: string, background: string) => {
    return odeServices.http().put('/userbook/api/preferences', {
      language: { 'default-domain': language },
      background,
    });
  },
};
