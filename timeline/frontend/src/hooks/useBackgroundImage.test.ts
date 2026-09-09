import { renderHook } from '@testing-library/react';
import { useBackgroundImage } from './useBackgroundImage';

const mocks = vi.hoisted(() => ({
  useUiOverride: vi.fn(),
  useUserPreferences: vi.fn(),
}));

vi.mock('@edifice.io/react', () => ({
  useUiOverride: mocks.useUiOverride,
  useUserPreferences: mocks.useUserPreferences,
}));

describe('useBackgroundImage', () => {
  beforeEach(() => {
    mocks.useUiOverride.mockReturnValue(undefined);
    mocks.useUserPreferences.mockReturnValue({ preferences: undefined });
  });

  it('uses the selected background from user preferences', () => {
    mocks.useUserPreferences.mockReturnValue({
      preferences: { background: 'blue-200' },
    });

    const { result } = renderHook(() => useBackgroundImage());

    expect(result.current.background).toBe('blue-200');
    expect(result.current.getBackgroundImgUrl('blue-200')).toContain(
      'blue-200.png',
    );
  });

  it('falls back to the default background when preferences are unavailable', () => {
    const { result } = renderHook(() => useBackgroundImage());

    expect(result.current.background).toBe('default');
    expect(result.current.getBackgroundImgUrl('default')).toContain(
      'default.png',
    );
  });

  it('uses the custom default background override', () => {
    mocks.useUiOverride.mockReturnValue({
      variant: 'https://example.test/custom-background.png',
    });

    const { result } = renderHook(() => useBackgroundImage());

    expect(result.current.getBackgroundImgUrl('default')).toBe(
      'https://example.test/custom-background.png',
    );
    expect(result.current.backgroundImgStyle).toMatchObject({
      backgroundImage: 'url(https://example.test/custom-background.png)',
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
    });
  });

  it('does not apply the custom default override to another background', () => {
    mocks.useUiOverride.mockReturnValue({
      variant: 'https://example.test/custom-background.png',
    });

    const { result } = renderHook(() => useBackgroundImage());

    expect(result.current.getBackgroundImgUrl('pink-200')).toContain(
      'pink-200.png',
    );
  });
});
