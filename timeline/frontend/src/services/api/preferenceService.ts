import { odeServices } from '@edifice.io/client';

type UserPrefs = { homePage: { betaEnabled: boolean } | null };

export const preferenceService = {
  deactivateHomepage: () => {
    return odeServices.http().put<UserPrefs>('/userbook/api/preferences', {
      homePage: { betaEnabled: false },
    });
  },
};
