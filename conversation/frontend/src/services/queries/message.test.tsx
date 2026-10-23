import { renderHook, waitFor } from '@testing-library/react';

import { act } from 'react';
import { wrapper } from '~/mocks/setup';
import { MessageBase } from '~/models';
import { queryClient } from '~/providers';
import { messageService } from '../api';
import { folderQueryKeys } from './folder';
import {
  useDeleteMessage,
  useMarkRead,
  useMarkUnread,
  useMoveMessage,
  useRestoreMessage,
  useTrashMessage,
} from './message';

describe('Message Queries', () => {
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

  test('use useTrashMessage hook to move messages to trash', async () => {
    const { result } = renderHook(() => useTrashMessage(), { wrapper });

    const messageServiceSpy = vi.spyOn(messageService, 'moveToFolder');

    const variables = { id: ['1234', '5678'] };

    act(() => {
      result.current.mutate(variables);
    });

    await waitFor(() => {
      expect(messageServiceSpy).toHaveBeenCalledWith('trash', variables.id);
    });
  });

  test('use useMoveMessage hook to remove messages from their folder, refreshing every origin folder', async () => {
    const { result } = renderHook(() => useMoveMessage(), { wrapper });

    vi.spyOn(messageService, 'moveToFolder').mockResolvedValue(undefined);
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    act(() => {
      result.current.mutate({ folderId: 'inbox', id: ['1234', '5678'] });
    });

    await waitFor(() => {
      ['inbox', 'outbox', 'draft'].forEach((originFolderId) =>
        expect(invalidateSpy).toHaveBeenCalledWith({
          queryKey: folderQueryKeys.messages(originFolderId),
        }),
      );
    });
  });

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
