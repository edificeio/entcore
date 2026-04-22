import { odeServices } from '@edifice.io/client';

export async function fetchLanguages(): Promise<string[]> {
  return odeServices.http().get<string[]>('/languages');
}

export async function getLanguagePreference(): Promise<string | null> {
  const res = await odeServices
    .http()
    .get<{ preference: string }>('/userbook/preference/language');
  try {
    const parsed: { 'default-domain'?: string } = JSON.parse(res.preference);
    return parsed['default-domain'] ?? null;
  } catch {
    return null;
  }
}

export async function saveLanguagePreference(lang: string): Promise<void> {
  await odeServices
    .http()
    .putJson('/userbook/preference/language', { 'default-domain': lang });
}
