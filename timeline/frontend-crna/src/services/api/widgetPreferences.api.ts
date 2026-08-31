import { odeServices } from '@edifice.io/client';
import type { WidgetId } from '~/models/widgetPreferences';

const PREFERENCE_KEY = 'homepage-widgets';

export async function fetchHiddenWidgets(): Promise<WidgetId[]> {
  const res = await odeServices
    .http()
    .get<{ preference: string }>(`/userbook/preference/${PREFERENCE_KEY}`);
  try {
    const parsed: { hidden?: WidgetId[] } = JSON.parse(res.preference);
    return parsed.hidden ?? [];
  } catch {
    return [];
  }
}

export async function saveHiddenWidgets(hidden: WidgetId[]): Promise<void> {
  await odeServices
    .http()
    .putJson(`/userbook/preference/${PREFERENCE_KEY}`, { hidden });
}
