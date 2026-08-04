import { act, renderHook, waitFor } from '@testing-library/react';
import { absenceService } from '../api';
import { wrapper } from '~/mocks/setup';
import { mockAbsenceSettings } from '~/mocks';
import { queryClient } from '~/providers';
import {
  absenceQueryKeys,
  useAbsenceSettings,
  useSaveAbsenceSettings,
} from './absence';

describe('Absence Queries', () => {
  beforeEach(() => {
    queryClient.clear();
  });

  test('useAbsenceSettings fetches and normalizes the current settings', async () => {
    const serviceSpy = vi.spyOn(absenceService, 'getSettings');

    const { result } = renderHook(useAbsenceSettings, { wrapper });

    await waitFor(() => {
      expect(serviceSpy).toHaveBeenCalled();
      expect(result.current.data).toStrictEqual({
        enabled: mockAbsenceSettings.enabled,
        startAt: mockAbsenceSettings.startAt,
        endAt: mockAbsenceSettings.endAt,
        bodyJson: mockAbsenceSettings.bodyJson,
      });
    });
  });

  test('useAbsenceSettings returns null when the API returns an empty object', async () => {
    vi.spyOn(absenceService, 'getSettings').mockResolvedValueOnce({});

    const { result } = renderHook(useAbsenceSettings, { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(result.current.data).toBeNull();
  });

  test('useSaveAbsenceSettings saves the settings and optimistically updates the query cache', async () => {
    const serviceSpy = vi.spyOn(absenceService, 'saveSettings');

    const { result: mutation } = renderHook(useSaveAbsenceSettings, {
      wrapper,
    });

    const payload = {
      enabled: false,
      startAt: mockAbsenceSettings.startAt,
      endAt: mockAbsenceSettings.endAt,
      bodyJson: mockAbsenceSettings.bodyJson,
    };

    await act(async () => {
      await mutation.current.mutateAsync(payload);
    });

    expect(serviceSpy).toHaveBeenCalledWith(payload);
    expect(queryClient.getQueryData(absenceQueryKeys.settings())).toStrictEqual(
      payload,
    );
  });
});

describe('absenceQueryKeys', () => {
  test('builds hierarchical keys', () => {
    expect(absenceQueryKeys.all()).toStrictEqual(['absence']);
    expect(absenceQueryKeys.settings()).toStrictEqual(['absence', 'settings']);
  });
});
