import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services';
import {
  mockCountOfMessagesInInbox,
  mockFolderTree,
  mockMessagesOfInbox,
} from '..';

/**
 * MSW Handlers
 * Mock HTTP methods for folder service
 */
export const folderHandlers = [
  http.get(`${baseUrl}/api/folders`, () => {
    return HttpResponse.json(mockFolderTree, { status: 200 });
  }),
  http.get(`${baseUrl}/api/folders/:folderId/messages`, ({ params }) => {
    if (params['folderId'] != 'inbox') {
      return HttpResponse.text('Unexpected error', { status: 500 });
    } else {
      return HttpResponse.json(mockMessagesOfInbox, { status: 200 });
    }
  }),
  http.get(`${baseUrl}/count/:folderId`, ({ params }) => {
    if (params['folderId'] != 'inbox') {
      return HttpResponse.text('Unexpected error', { status: 500 });
    } else {
      return HttpResponse.json(mockCountOfMessagesInInbox, { status: 200 });
    }
  }),
  http.post(`${baseUrl}/folder`, () => {
    return HttpResponse.json({ id: 'folder_Z' }, { status: 201 });
  }),
  http.put(`${baseUrl}/folder/:folderId`, () => {
    return HttpResponse.json({}, { status: 200 });
  }),
  http.put(`${baseUrl}/folder/trash/:folderId`, () => {
    return HttpResponse.json({}, { status: 200 });
  }),
];
