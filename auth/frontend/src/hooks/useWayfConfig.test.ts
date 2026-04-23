import { renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DEFAULT_WAYF_CONFIG, wayfConfig } from '~/config/wayf.config';
import { useWayfConfig } from './useWayfConfig';

describe('useWayfConfig', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns the domain config when hostname matches', () => {
    vi.spyOn(window, 'location', 'get').mockReturnValue({
      ...window.location,
      hostname: 'localhost',
    });

    const { result } = renderHook(() => useWayfConfig());
    expect(result.current).toBe(wayfConfig['wayf-v2']['localhost']);
  });

  it('falls back to DEFAULT_WAYF_CONFIG for unknown hostname', () => {
    vi.spyOn(window, 'location', 'get').mockReturnValue({
      ...window.location,
      hostname: 'unknown.example.com',
    });

    const { result } = renderHook(() => useWayfConfig());
    expect(result.current).toBe(DEFAULT_WAYF_CONFIG);
  });
});
