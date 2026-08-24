import { odeServices } from '@edifice.io/client';

export interface ThemeDesc {
  _id: string;
  displayName: string;
  path: string;
}

export async function fetchThemes(): Promise<ThemeDesc[]> {
  return odeServices.http().get<ThemeDesc[]>('/themes');
}

export async function fetchCurrentThemeName(): Promise<{
  themeName: string;
  skinName: string;
}> {
  const { themeName, skinName } = await odeServices
    .http()
    .get<{ themeName: string; skinName: string }>('/theme');
  return { themeName, skinName };
}

export async function saveThemePreference(
  themeName: string,
  themeId: string,
): Promise<void> {
  await odeServices
    .http()
    .get(
      `/userbook/api/edit-userbook-info?prop=theme-${themeName}&value=${themeId}`,
    );
}
