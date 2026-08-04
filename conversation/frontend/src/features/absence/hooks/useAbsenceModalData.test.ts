import { act, renderHook, waitFor } from '@testing-library/react';
import { mockAbsenceSettings } from '~/mocks';
import { wrapper } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { absenceService } from '~/services';
import { useAbsenceModalData } from './useAbsenceModalData';

describe('useAbsenceModalData', () => {
  beforeEach(() => {
    queryClient.clear();
  });

  it('starts loading, then settles with the fetched settings', async () => {
    const { result } = renderHook(useAbsenceModalData, { wrapper });

    expect(result.current.isLoadingSettings).toBe(true);
    expect(result.current.settings).toBeUndefined();

    await waitFor(() => expect(result.current.isLoadingSettings).toBe(false));

    expect(result.current.settings).toStrictEqual({
      enabled: mockAbsenceSettings.enabled,
      startAt: mockAbsenceSettings.startAt,
      endAt: mockAbsenceSettings.endAt,
      bodyJson: mockAbsenceSettings.bodyJson,
    });
  });

  it('saves through the mutation and reflects its pending state', async () => {
    const saveSpy = vi.spyOn(absenceService, 'saveSettings');
    const { result } = renderHook(useAbsenceModalData, { wrapper });
    await waitFor(() => expect(result.current.isLoadingSettings).toBe(false));

    const payload = {
      enabled: false,
      startAt: mockAbsenceSettings.startAt,
      endAt: mockAbsenceSettings.endAt,
      bodyJson: mockAbsenceSettings.bodyJson,
    };

    expect(result.current.isSaving).toBe(false);
    await act(async () => result.current.handleSave(payload));

    expect(saveSpy).toHaveBeenCalledWith(payload);
    expect(result.current.isSaving).toBe(false);
  });
});
