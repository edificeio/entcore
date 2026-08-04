import { HttpResponse, delay, http } from 'msw';
import { mockAbsenceSettings, setMockAbsenceSettings } from '../datas/absences';
import { baseUrl } from '~/services';

/**
 * MSW Handlers
 * Mock HTTP methods for the absence service
 */
export const absenceHandlers = [
  http.get(`${baseUrl}/absence`, async () => {
    // Only in `dev:mock` (never in tests): lets the loading state (skeleton)
    // be observed instead of resolving instantly.
    if (import.meta.env.MODE === 'mock') {
      await delay(1000);
    }
    return HttpResponse.json(mockAbsenceSettings, { status: 200 });
  }),

  http.put(`${baseUrl}/absence`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    // Only in `dev:mock` (never in tests): lets the saving state (spinner)
    // be observed instead of resolving instantly.
    if (import.meta.env.MODE === 'mock') {
      await delay(1000);
    }
    const next = {
      ...mockAbsenceSettings,
      ...body,
      bodyHtml: mockAbsenceSettings.bodyHtml,
      updatedAt: new Date().toISOString(),
    };
    setMockAbsenceSettings(next);
    return HttpResponse.json(next, { status: 200 });
  }),
];
