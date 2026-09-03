import { IThemeDesc, odeServices } from '@edifice.io/client';
import { preferenceService } from './preferenceService';

export type Background =
  | 'default'
  | 'pink-200'
  | 'yellow-200'
  | 'orange-200'
  | 'blue-200'
  | 'green-200';

export const customizeService = {
  listLanguages: () => odeServices.http().get<string[]>('/languages'),

  listBackgrounds: () =>
    Promise.resolve<Background[]>([
      'default',
      'pink-200',
      'yellow-200',
      'orange-200',
      'blue-200',
      'green-200',
    ]),

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
