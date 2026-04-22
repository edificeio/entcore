import { odeServices } from '@edifice.io/client';

export interface ThemeDesc {
  _id: string;
  displayName: string;
  path: string;
}

export interface CurrentTheme {
  themeName: string;
  skinName: string;
}

export async function fetchThemes(): Promise<ThemeDesc[]> {
  return odeServices.http().get<ThemeDesc[]>('/themes');
}

export async function fetchCurrentTheme(): Promise<CurrentTheme> {
  return odeServices.http().get<CurrentTheme>('/theme');
}

export async function saveThemePreference(
  themeName: string,
  skinId: string,
): Promise<void> {
  await odeServices
    .http()
    .get(
      `/userbook/api/edit-userbook-info?prop=theme-${themeName}&value=${skinId}`,
    );
}
