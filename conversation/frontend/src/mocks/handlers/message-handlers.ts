import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services';
import {
  mockFullMessage,
  mockSentMessage,
} from '..';

/**
 * MSW Handlers
 * Mock HTTP methods for folder service
 */
export const messageHandlers = [
  // Message service
  http.get(`${baseUrl}/api/messages/:messageId`, () => {
    return HttpResponse.json(mockFullMessage, { status: 200 });
  }),
  http.post<
    object,
    {
      id: string[];
      unread: boolean;
    }
  >(`${baseUrl}/toggleUnread`, async ({ request }) => {
    const payload = await request.json();
    if (
      !payload ||
      !Array.isArray(payload.id) ||
      typeof payload.unread !== 'boolean'
    ) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.put<
    object,
    {
      id: string[];
    }
  >(`${baseUrl}/conversation/restore`, async ({ request }) => {
    console.log('trash test');
    const payload = await request.json();
    if (!payload || !Array.isArray(payload.id)) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.put<
    object,
    {
      id: string[];
    }
  >(`${baseUrl}/conversation/trash`, async ({ request }) => {
    const payload = await request.json();
    if (!payload || !Array.isArray(payload.id)) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.put<
    object,
    {
      id: string[];
    }
  >(`${baseUrl}/conversation/delete`, async ({ request }) => {
    const payload = await request.json();
    if (!payload || !Array.isArray(payload.id)) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.post(`${baseUrl}/conversation/draft`, () => {
    return HttpResponse.json({ id: 'message_draft' }, { status: 201 });
  }),
  http.post(`${baseUrl}/conversation/draft/:draftId`, () => {
    return HttpResponse.text('', { status: 200 });
  }),
  http.post(`${baseUrl}/conversation/send`, async ({ request }) => {
    const url = new URL(request.url);
    const id = url.searchParams.get('id');
    const payload = await request.json();
    if (!id) {
      return HttpResponse.text('Bad Request', {
        status: 400,
      });
    }
    return HttpResponse.json(Object.assign({ id }, mockSentMessage, payload), {
      status: 200,
    });
  }),
];
