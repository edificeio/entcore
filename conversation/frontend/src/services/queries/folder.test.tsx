import { renderHook, waitFor } from '@testing-library/react';
import { useFolderMessages } from './folder';
import { folderService } from '../api';
import { wrapper } from '~/mocks/setup';

describe('Folder Queries', () => {
  test('use useFolderMessages hook to get all messages for a specific folder', async () => {
    const folderServiceSpy = vi.spyOn(folderService, 'getMessages');

    renderHook(() => useFolderMessages('1234'), {
      wrapper,
    });

    await waitFor(() => {
      expect(folderServiceSpy).toHaveBeenCalledWith('1234', {
        search: undefined,
        unread: undefined,
        page: 0,
        pageSize: 20,
      });
    });
  });

  test('use useFolderMessages hook to get all messages for a specific folder with a search and filter', async () => {
    const folderServiceSpy = vi.spyOn(folderService, 'getMessages');

    renderHook(() => useFolderMessages('inbox'), {
      wrapper: ({ children }: { children: React.ReactNode }) =>
        wrapper({
          initialEntries: ['?search=searchValue&unread=true'],
          children,
        }),
    });

    await waitFor(() => {
      expect(folderServiceSpy).toHaveBeenCalledWith('inbox', {
        search: 'searchValue',
        unread: true,
        page: 0,
        pageSize: 20,
      });
    });
  });
});
