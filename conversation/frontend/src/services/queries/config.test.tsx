import { renderHook, waitFor } from '@testing-library/react';
import { configService } from '../api';
import { wrapper } from '~/mocks/setup';
import { useGlobalConfig } from './config';

describe('Config Queries', () => {
  test('use useGlobalConfig hook to get... the global config :)', async () => {
    const serviceSpy = vi.spyOn(configService, 'getGlobalConfig');

    renderHook(useGlobalConfig, {
      wrapper,
    });

    await waitFor(() => {
      expect(serviceSpy).toHaveBeenCalled();
    });
  });
});

// TODO Signature tests
