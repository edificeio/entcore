import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services';
import { inboxMessages } from '../data/messages/list';
import * as messagesDetails from '../data/messages/details';
console.log('messagesDetails:', messagesDetails);

/**
 * MSW Handlers
 * Mock HTTP methods for folder service
 */
export const tempHandlers = [
  // Message service
  http.get(`${baseUrl}/api/folders/inbox/messages`, () => {
    // This also covers the ?originalFormat=true query parameter
    return HttpResponse.json(inboxMessages, { status: 200 });
  }),
  ...Object.values(messagesDetails).map((messageDetails) => {
    console.log('messageDetail:', messageDetails);
    const { id } = messageDetails;
    return http.get(`${baseUrl}/api/messages/${id}`, () => {
      // This also covers the ?originalFormat=true query parameter
      return HttpResponse.json(messageDetails, { status: 200 });
    });
  }),
];
