import { IThemeDesc, odeServices } from '@edifice.io/client';
import { preferenceService } from './preferenceService';

export const customizeService = {
  listLanguages: () => odeServices.http().get<string[]>('/languages'),

  listBackgrounds: () => odeServices.http().get<string[]>('/backgrounds'),

  listFonts: () => odeServices.http().get<IThemeDesc[]>('/themes'),

  // Inherited from legacy theme/skin feature. A skin is now a font, but it remains paired to a theme.
  getThemeAndSkin: () =>
    odeServices.http().get<{ themeName: string; skinName: string }>('/theme'),

  save: ({
    language,
    themeName,
    font,
    background,
  }: {
    language: string;
    themeName: string;
    font: string;
    background: string;
  }) =>
    Promise.all([
      preferenceService.saveCustomization(language, background),
      odeServices
        .http()
        .get(
          `/userbook/api/edit-userbook-info?prop=theme-${themeName}&value=${font}`,
        ),
    ]),
};
