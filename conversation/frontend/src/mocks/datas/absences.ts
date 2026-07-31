import { AbsenceSettingsResponse } from '~/models/absence';

function startOfLocalDayUtc(date: Date): string {
  const value = new Date(date);
  value.setHours(0, 0, 0, 0);
  return value.toISOString();
}

function endOfLocalDayUtc(date: Date): string {
  const value = new Date(date);
  value.setHours(23, 59, 59, 999);
  return value.toISOString();
}

const mockAbsenceStart = new Date();
const mockAbsenceEnd = new Date(mockAbsenceStart);
mockAbsenceEnd.setDate(mockAbsenceEnd.getDate() + 7);

/** Always active: today through today + 7 days, so `dev:mock` shows an active absence. */
export let mockAbsenceSettings: AbsenceSettingsResponse = {
  enabled: true,
  startAt: startOfLocalDayUtc(mockAbsenceStart),
  endAt: endOfLocalDayUtc(mockAbsenceEnd),
  bodyJson: {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [
          {
            type: 'text',
            text: 'Je suis absent(e) du 1er au 31 août 2026. Je ne serai pas en mesure de répondre à vos messages durant cette période.',
          },
        ],
      },
    ],
  },
  bodyHtml:
    '<p>Je suis absent(e) du 1er au 31 août 2026. Je ne serai pas en mesure de répondre à vos messages durant cette période.</p>',
  updatedAt: new Date().toISOString(),
};

/** Persists a `PUT` in the mock so a subsequent `GET` reflects the last save. */
export function setMockAbsenceSettings(next: AbsenceSettingsResponse): void {
  mockAbsenceSettings = next;
}
