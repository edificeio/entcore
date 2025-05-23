import { renderHook, waitFor } from '@testing-library/react';

import { act } from 'react';
import { wrapper } from '~/mocks/setup';
import { MessageBase } from '~/models';
import { messageService } from '../api';
import {
  useDeleteMessage,
  useMarkRead,
  useMarkUnread,
  useRestoreMessage,
  useTrashMessage,
} from './message';

// const mocks = vi.hoisted(() => ({
//   useSelectedFolder: vi.fn(),
// }));

// vi.mock('~/hooks/useSelectedFolder', () => ({
//   useSelectedFolder: mocks.useSelectedFolder,
// }));

describe('Message Queries', () => {
  // beforeEach(() => {
  //   mocks.useSelectedFolder.mockReturnValue({ folderId: 'trash' });
  // });

  // afterEach(() => {
  //   vi.clearAllMocks();
  // });

  test('use useMarkRead hook to mark as read messages', async () => {
    const { result } = renderHook(() => useMarkRead(), {
      wrapper,
    });

    const messageServiceSpy = vi.spyOn(messageService, 'toggleUnread');

    const variables = {
      messages: [{ id: '1234', unread: true }] as MessageBase[],
    };

    act(() => {
      result.current.mutate(variables);
    });

    await waitFor(() => {
      expect(messageServiceSpy).toHaveBeenCalledWith(
        variables.messages.map((m) => m.id),
        false,
      );
    });
  });

  test('use useMarkUnread hook to mark as unread messages', async () => {
    const { result } = renderHook(() => useMarkUnread(), {
      wrapper,
    });

    const messageServiceSpy = vi.spyOn(messageService, 'toggleUnread');

    const variables = {
      messages: [
        { id: '1234', unread: false },
        { id: '15619', unread: false },
      ] as MessageBase[],
    };

    act(() => {
      result.current.mutate(variables);
    });

    await waitFor(() => {
      expect(messageServiceSpy).toHaveBeenCalledWith(
        variables.messages.map((m) => m.id),
        true,
      );
    });
  });

  // test.only('use useTrashMessage hook to move messages to trash', async () => {
  //   // mocks.useSelectedFolder.mockReturnValue({ folderId: 'trash' });

  //   const { result } = renderHook(() => useTrashMessage(), { wrapper });

  //   const messageServiceSpy = vi.spyOn(messageService, 'moveToFolder');

  //   const variables = { id: ['1234', '5678'] };

  //   act(() => {
  //     result.current.mutate(variables);
  //   });

  //   await waitFor(() => {
  //     expect(messageServiceSpy).toHaveBeenCalledWith('trash', variables.id);
  //   });
  // });

  test('use useRestoreMessage hook to restore messages from trash', async () => {
    const { result } = renderHook(() => useRestoreMessage(), { wrapper });

    const messageServiceSpy = vi.spyOn(messageService, 'restore');

    const variables = { id: ['1234', '5678'] };

    act(() => {
      result.current.mutate(variables);
    });

    await waitFor(() => {
      expect(messageServiceSpy).toHaveBeenCalledWith(variables.id);
    });
  });

  test('use useDeleteMessage hook to delete messages', async () => {
    const { result } = renderHook(() => useDeleteMessage(), { wrapper });

    const messageServiceSpy = vi.spyOn(messageService, 'delete');

    const variables = { id: ['1234', '5678'] };

    act(() => {
      result.current.mutate(variables);
    });

    await waitFor(() => {
      expect(messageServiceSpy).toHaveBeenCalledWith(variables.id);
    });
  });
});
