import { renderHook } from '@testing-library/react';
import { useHomepageWidgets } from './useHomepageWidgets';

const mocks = vi.hoisted(() => ({
  useWidgetPreferences: vi.fn(),
  useWidgetsOrder: vi.fn(),
}));

vi.mock('./useWidgetsOrder', () => ({
  useWidgetsOrder: mocks.useWidgetsOrder,
}));

vi.mock('./useWidgetPreferences', () => ({
  useWidgetPreferences: mocks.useWidgetPreferences,
}));

vi.mock('@edifice.io/react/homepage', () => {
  const stub = () => null;
  return {
    AgendaContainer: stub,
    BriefMeContainer: stub,
    CantineContainer: stub,
    CommunitiesContainer: stub,
    EdumalinContainer: stub,
    GenerationHdfContainer: stub,
    QwantContainer: stub,
    RssContainer: stub,
    UniversalisContainer: stub,
    UsefulLinksContainer: stub,
  };
});

const setPreferences = ({
  deployed,
  visible = () => true,
  locked = () => false,
}: {
  deployed: string[];
  visible?: (name: string) => boolean;
  locked?: (name: string) => boolean;
}) =>
  mocks.useWidgetPreferences.mockReturnValue({
    widgets: deployed.map((name) => ({ name })),
    isVisible: visible,
    isLocked: locked,
  });

const setOrder = (names: string[], isLoading = false) =>
  mocks.useWidgetsOrder.mockReturnValue({
    order: names.map((name) => ({ name })),
    isLoading,
  });

const names = (list: { name: string }[]) => list.map((w) => w.name);

describe('useHomepageWidgets', () => {
  beforeEach(() => {
    setOrder([
      'communities-widget',
      'agenda-widget',
      'universalis-widget',
      'cantine-widget',
    ]);
  });

  it('lists the deployed catalog widgets in the configured order', () => {
    // Deliberately given in another order than the configured one
    setPreferences({
      deployed: [
        'cantine-widget',
        'universalis-widget',
        'agenda-widget',
        'communities-widget',
      ],
    });

    const { result } = renderHook(() => useHomepageWidgets());

    expect(names(result.current.available)).toEqual([
      'communities-widget',
      'agenda-widget',
      'universalis-widget',
      'cantine-widget',
    ]);
  });

  it('follows the order given by the project', () => {
    setOrder(['cantine-widget', 'agenda-widget']);
    setPreferences({ deployed: ['agenda-widget', 'cantine-widget'] });

    const { result } = renderHook(() => useHomepageWidgets());

    expect(names(result.current.available)).toEqual([
      'cantine-widget',
      'agenda-widget',
    ]);
  });

  it('hides deployed widgets absent from the order', () => {
    setOrder(['agenda-widget']);
    setPreferences({ deployed: ['agenda-widget', 'cantine-widget'] });

    const { result } = renderHook(() => useHomepageWidgets());

    expect(names(result.current.available)).toEqual(['agenda-widget']);
    expect(names(result.current.displayed)).toEqual(['agenda-widget']);
  });

  it('lists nothing while the order is loading', () => {
    setOrder(['agenda-widget'], true);
    setPreferences({ deployed: ['agenda-widget'] });

    const { result } = renderHook(() => useHomepageWidgets());

    expect(result.current.isLoading).toBe(true);
    expect(result.current.available).toEqual([]);
  });

  it('ignores deployed widgets the new homepage does not handle', () => {
    setOrder(['agenda-widget', 'mood', 'notes']);
    setPreferences({ deployed: ['agenda-widget', 'mood', 'notes'] });

    const { result } = renderHook(() => useHomepageWidgets());

    expect(names(result.current.available)).toEqual(['agenda-widget']);
  });

  it('lists nothing when no handled widget is deployed', () => {
    setPreferences({ deployed: [] });

    const { result } = renderHook(() => useHomepageWidgets());

    expect(result.current.available).toEqual([]);
    expect(result.current.displayed).toEqual([]);
  });

  it('displays widgets only when deployed and not hidden', () => {
    setPreferences({
      deployed: ['agenda-widget', 'universalis-widget'],
      visible: (name) => name !== 'agenda-widget',
    });

    const { result } = renderHook(() => useHomepageWidgets());

    const displayed = names(result.current.displayed);
    expect(displayed).toContain('universalis-widget');
    expect(displayed).not.toContain('agenda-widget');
    expect(displayed).not.toContain('communities-widget');
  });

  it('keeps a locked widget displayed whatever its preference', () => {
    setPreferences({
      deployed: ['agenda-widget'],
      visible: () => false,
      locked: (name) => name === 'agenda-widget',
    });

    const { result } = renderHook(() => useHomepageWidgets());

    expect(names(result.current.displayed)).toContain('agenda-widget');
  });
});
