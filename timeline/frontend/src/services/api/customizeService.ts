import { IThemeDesc, odeServices } from '@edifice.io/client';

export const customizeService = {
  listLanguages: () => odeServices.http().get<string[]>('/languages'),

  listThemes: () => odeServices.http().get<IThemeDesc[]>('/themes'),

  // ctrl.themeSvc.getBootstrapThemePath()
};
