import { renderHook, waitFor } from '@testing-library/react';
import { describe, expect, test } from 'vitest';

import { useMarkRead } from './message';
import { wrapper } from '~/mocks/setup';
import { act } from 'react';

describe('Message Queries', () => {
  test('use useMarkRead hook to mark as read messages', async () => {
    const { result } = renderHook(() => useMarkRead(), {
      wrapper,
    });

    const variables = { id: '1234' };
    
    act(() => {
      result.current.mutate(variables);
    } );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
      expect(result.current.data).toBeUndefined();
    });
  });
});
