import { act, renderHook, waitFor } from '@testing-library/react';
import { mockAbsenceSettings } from '~/mocks';
import { wrapper } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { absenceService } from '~/services';
import { isAbsenceActive, useAbsenceReminder } from './useAbsenceReminder';

const activeSettings = {
  enabled: true,
  startAt: '2026-01-01T00:00:00.000Z',
  endAt: '2026-01-10T00:00:00.000Z',
  bodyJson: { type: 'doc', content: [] },
};

describe('isAbsenceActive', () => {
  it('is false when there are no settings', () => {
    expect(isAbsenceActive(null)).toBe(false);
    expect(isAbsenceActive(undefined)).toBe(false);
  });

  it('is false when disabled, even within the date range', () => {
    expect(
      isAbsenceActive(
        { ...activeSettings, enabled: false },
        new Date('2026-01-05T00:00:00.000Z'),
      ),
    ).toBe(false);
  });

  it('is false before startAt or after endAt', () => {
    expect(
      isAbsenceActive(activeSettings, new Date('2025-12-31T00:00:00.000Z')),
    ).toBe(false);
    expect(
      isAbsenceActive(activeSettings, new Date('2026-01-11T00:00:00.000Z')),
    ).toBe(false);
  });

  it('is true within [startAt, endAt] while enabled', () => {
    expect(
      isAbsenceActive(activeSettings, new Date('2026-01-05T00:00:00.000Z')),
    ).toBe(true);
  });
});

describe('useAbsenceReminder', () => {
  beforeEach(() => {
    queryClient.clear();
  });

  it('reflects an active absence from the settings, and toggles the modal state', async () => {
    const { result } = renderHook(useAbsenceReminder, { wrapper });

    await waitFor(() => expect(result.current.isActive).toBe(true));
    expect(result.current.isModalOpen).toBe(false);

    act(() => result.current.openModal());
    expect(result.current.isModalOpen).toBe(true);

    act(() => result.current.closeModal());
    expect(result.current.isModalOpen).toBe(false);
  });

  it('is not active when the settings are disabled', async () => {
    vi.spyOn(absenceService, 'getSettings').mockResolvedValueOnce({
      ...mockAbsenceSettings,
      enabled: false,
    });

    const { result } = renderHook(useAbsenceReminder, { wrapper });

    await waitFor(() => expect(result.current.isActive).toBe(false));
  });
});
