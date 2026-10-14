import { renderHook, waitFor } from '~/mocks/setup';
import { Providers } from '~/providers';
import { useCustomization } from './useCustomization';

const mocks = vi.hoisted(() => ({
  uiOverrides: {} as Record<string, { variant: string } | undefined>,
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );

  return {
    ...actual,
    useUiOverride: (key: string) => mocks.uiOverrides[key],
  };
});

describe('useCustomization', () => {
  afterEach(() => {
    mocks.uiOverrides = {};
  });

  it('replaces the generic default background with the platform default', async () => {
    mocks.uiOverrides['background.default'] = { variant: 'crna' };

    const { result } = renderHook(() => useCustomization(), {
      wrapper: Providers,
    });

    await waitFor(() =>
      expect(result.current.backgrounds).toEqual([
        'crna',
        'pink-200',
        'yellow-200',
        'orange-200',
        'blue-200',
        'green-200',
      ]),
    );
  });

  it('keeps the generic default background when no platform default is set', async () => {
    const { result } = renderHook(() => useCustomization(), {
      wrapper: Providers,
    });

    await waitFor(() =>
      expect(result.current.backgrounds?.[0]).toBe('default'),
    );
  });
});
