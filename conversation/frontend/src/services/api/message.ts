import { odeServices } from 'edifice-ts-client';

import { Message } from '~/models';

/**
 * Fully load a message.
 * @returns
 */
export function loadMessage(id: string) {
  return odeServices.http().get<Message[]>(`/conversation/api/messages/${id}`);
}
