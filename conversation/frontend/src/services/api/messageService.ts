import { NOOP } from '@edifice.io/utilities';
import { odeServices } from 'edifice-ts-client';

import { Message, MessageSentResponse } from '~/models';

/** Utility function to map one or more IDs to an array of IDs */
function asArray(ids: string | string[]): string[] {
  return typeof ids == 'string' ? [ids] : ids;
}

/**
 * Utility function to HTTP PUT an optional payload to an endoint.
 * Then discard the response to void.
 */
function putThenVoid(endpoint: string, payload?: any) {
  return odeServices.http().put<object>(endpoint, payload).then(NOOP);
}

/**
 * Utility function to HTTP POST an optional payload to an endoint.
 * Then discard the response to void.
 */
function postThenVoid(endpoint: string, payload?: any) {
  return odeServices.http().post<object>(endpoint, payload).then(NOOP);
}

/**
 * Creates a message service with the specified base URL.
 *
 * @param baseURL The base URL for the message service API.
 * @returns A service to interact with messages.
 */
export const createMessageService = (baseURL: string) => ({
  /**
   * Fully load a message.
   */
  getById(id: string) {
    return odeServices.http().get<Message>(`${baseURL}/api/messages/${id}`);
  },

  /**
   * Fully load a message by ID, in original format.
   */
  getOriginalFormat(id: string) {
    return odeServices
      .http()
      .get<Message>(`${baseURL}/api/messages/${id}?originalFormat=true`);
  },

  /** Toggle one or more messages as `read` or `unread`. */
  toggleUnread(ids: string | string[], unread = true) {
    return postThenVoid(`${baseURL}/toggleUnread`, {
      id: asArray(ids),
      unread,
    });
  },

  /** Restore one or more messages from trash bin. */
  restore(ids: string | string[]) {
    return putThenVoid(`${baseURL}/restore`, {
      id: asArray(ids),
    });
  },

  /** Permanently delete one or more messages. */
  delete(ids: string | string[]) {
    return putThenVoid(`${baseURL}/delete`, {
      id: asArray(ids),
    });
  },

  /** Empty the trash bin. */
  emptyTrash() {
    return odeServices.http().delete(`${baseURL}/emptyTrash`);
  },

  /**
   * Move one or more messages into a user-created folder, or "inbox", or "trash".
   * @param targetFolderId ID of a user-created folder, or "inbox" or "trash".
   * @param ids One or more messages ID to move to the target folder.
   */
  moveToFolder(
    targetFolderId: 'inbox' | 'trash' | string,
    ids: string | string[],
  ) {
    if (!targetFolderId) return Promise.reject('folderpicker.move.empty.text');
    const payload = {
      id: asArray(ids),
    };
    switch (targetFolderId.toLowerCase()) {
      case 'trash':
        return putThenVoid(`${baseURL}/trash`, payload);
      case 'inbox':
        return putThenVoid(`${baseURL}/move/root?id=${payload.id.join()}`);
      default:
        return putThenVoid(
          `${baseURL}/move/userfolder/${targetFolderId}`,
          payload,
        );
    }
  },

  /**
   * Send an existing draft email, optionaly updating its subject, body and/or recipients.
   * @returns up-to-date information about the sent email and its recipients.
   */
  send(
    draftId: string,
    payload?: {
      subject?: string;
      body?: string;
      /** IDs of recipients */
      to?: string[];
      /** IDs of recipients in "copie-carbone" */
      cc?: string[];
      /** IDs of recipients in "copie-carbone-invisible" */
      cci?: string[];
    },
  ) {
    return odeServices
      .http()
      .post<MessageSentResponse>(`${baseURL}/send?id=${draftId}`, payload);
  },

  recall(messageId: string) {
    return odeServices
      .http()
      .post<void>(`${baseURL}/api/messages/${messageId}/recall`);
  },

  createDraft(payload: {
    subject?: string;
    body?: string;
    to?: string[];
    cc?: string[];
    cci?: string[];
  }) {
    return odeServices.http().post<{ id: string }>(`${baseURL}/draft`, payload);
  },

  updateDraft(
    draftId: string,
    payload: {
      subject?: string;
      body?: string;
      to?: string[];
      cc?: string[];
      cci?: string[];
    },
  ) {
    return putThenVoid(`${baseURL}/draft/${draftId}`, payload);
  },
});
