/**
 * In-memory store for `/userbook/preference/:id` (GET returns the current
 * value, PUT overwrites it) so that stateful preferences — the notification
 * type filter, widget layout, language — persist across refetches within a
 * single dev/test session, exactly like the real backend does.
 */
const defaultPreferences: Record<string, unknown> = {
  apps: { bookmarks: [], applications: ['FakeApp'] },
  language: { 'default-domain': 'fr' },
  rgpdCookies: { showInfoTip: false },
  timeline: { type: [], page: 0 },
  widgets: {},
};

const store = new Map<string, unknown>(Object.entries(defaultPreferences));

export function getPreference(key: string): unknown {
  return store.has(key) ? store.get(key) : {};
}

export function setPreference(key: string, value: unknown): void {
  store.set(key, value);
}
