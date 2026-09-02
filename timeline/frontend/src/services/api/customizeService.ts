import { IThemeDesc, odeServices } from '@edifice.io/client';
import { preferenceService } from './preferenceService';

export const customizeService = {
  listLanguages: () => odeServices.http().get<string[]>('/languages'),

  listBackgrounds: () => odeServices.http().get<string[]>('/backgrounds'),

  listFonts: () => odeServices.http().get<IThemeDesc[]>('/themes'),

  // Inherited from legacy theme/skin feature. A skin is now a font, but it remains paired to a theme.
  getThemeAndSkin: () =>
    odeServices.http().get<{ themeName: string; skinName: string }>('/theme'),

  saveLanguagePreference: (lang: string) =>
    odeServices.conf().savePreference('language', {
      'default-domain': lang,
    }),

  // Needs a legacy theme to be applied.
  saveFontPreference: (themeName: string, font: string) =>
    odeServices
      .http()
      .get(
        `/userbook/api/edit-userbook-info?prop=theme-${themeName}&value=${font}`,
      ),

  saveBackgroundPreference: (background: string) =>
    preferenceService.saveBackground(background),
};
