import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services';
import { mockConfiguration, signaturePreferences } from '..';

/**
 * MSW Handlers
 * Mock HTTP methods for config service
 */
export const configHandlers = [
  http.get(`${baseUrl}/max-depth`, () => {
    return HttpResponse.json(mockConfiguration, { status: 200 });
  }),
  http.get(`/userbook/preference/conversation`, () =>
    HttpResponse.json(signaturePreferences, { status: 200 }),
  ),
  http.put(`/userbook/preference/conversation`, () =>
    HttpResponse.json({}, { status: 204 }),
  ),
];
