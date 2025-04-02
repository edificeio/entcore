import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services';
import { mockConfiguration } from '..';

/**
 * MSW Handlers
 * Mock HTTP methods for config service
 */
export const configHandlers = [
  http.get(`${baseUrl}/max-depth`, () => {
    return HttpResponse.json(mockConfiguration, { status: 200 });
  }),
];
