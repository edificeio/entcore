import { IThemeDesc, odeServices } from '@edifice.io/client';

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

  saveSkin: (themeName: string, font: string) =>
    odeServices
      .http()
      .get(
        `/userbook/api/edit-userbook-info?prop=theme-${themeName}&value=${font}`,
      ),
};
