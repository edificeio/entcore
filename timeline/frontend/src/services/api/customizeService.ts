import { IThemeDesc, odeServices } from '@edifice.io/client';

export const customizeService = {
  listLanguages: () => odeServices.http().get<string[]>('/languages'),

  listFonts: () => odeServices.http().get<IThemeDesc[]>('/themes'),

  saveLanguagePreference: (lang: string) =>
    odeServices.conf().savePreference('language', {
      'default-domain': lang,
    }),
};
