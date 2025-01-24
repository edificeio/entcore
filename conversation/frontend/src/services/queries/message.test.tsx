import { renderHook, waitFor } from '@testing-library/react';

import { useMarkRead, useMarkUnread } from './message';
import { wrapper } from '~/mocks/setup';
import { act } from 'react';
import { messageService } from '../api';

describe('Message Queries', () => {
  test('use useMarkRead hook to mark as read messages', async () => {
    const { result } = renderHook(() => useMarkRead(), {
      wrapper,
    });

    const messageServiceSpy = vi.spyOn(messageService, 'toggleUnread');

    const variables = { id: '1234' };

    act(() => {
      result.current.mutate(variables);
    });

    await waitFor(() => {
      expect(messageServiceSpy).toHaveBeenCalledWith(variables.id, false);
    });
  });

  test('use useMarkUnread hook to mark as unread messages', async () => {
    const { result } = renderHook(() => useMarkUnread(), {
      wrapper,
    });

    const messageServiceSpy = vi.spyOn(messageService, 'toggleUnread');

    const variables = { id: ['1234', '15619'] };

    act(() => {
      result.current.mutate(variables);
    });

    await waitFor(() => {
      expect(messageServiceSpy).toHaveBeenCalledWith(variables.id, true);
    });
  });
});
