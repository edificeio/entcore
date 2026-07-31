import { HttpResponse, http } from 'msw';
import { mockAbsenceSettings } from '../datas/absences';
import { baseUrl } from '~/services';

/**
 * MSW Handlers
 * Mock HTTP methods for the absence service
 */
export const absenceHandlers = [
  http.get(`${baseUrl}/absence`, () => {
    return HttpResponse.json(mockAbsenceSettings, { status: 200 });
  }),

  http.put(`${baseUrl}/absence`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json(
      {
        ...mockAbsenceSettings,
        ...body,
        bodyHtml: mockAbsenceSettings.bodyHtml,
        updatedAt: mockAbsenceSettings.updatedAt,
      },
      { status: 200 },
    );
  }),
];
