import { render, screen } from '@testing-library/react';
import type { WidgetsPersonalizationPanelProps } from '@edifice.io/react/homepage';
import { WidgetsPersonalizationPanelContainer } from './WidgetsPersonalizationPanelContainer';

const mocks = vi.hoisted(() => ({
  translations: {} as Record<string, string>,
  useWidgetPreferences: vi.fn(),
  useHomepageWidgets: vi.fn(),
}));

vi.mock('~/hooks/useHomepageWidgets', () => ({
  useHomepageWidgets: mocks.useHomepageWidgets,
}));

vi.mock('~/hooks/useWidgetPreferences', () => ({
  useWidgetPreferences: mocks.useWidgetPreferences,
}));

vi.mock('~/hooks/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, opts?: Record<string, unknown> | string) =>
      mocks.translations[key] ??
      (typeof opts === 'string'
        ? opts
        : ((opts?.defaultValue as string) ?? key)),
    common_t: (key: string) => key,
  }),
}));

// Isolate the container's own mapping/wiring logic from the shared,
// framework-provided panel UI (already covered by its own tests).
vi.mock('@edifice.io/react/homepage', () => ({
  WidgetsPersonalizationPanel: (props: WidgetsPersonalizationPanelProps) => (
    <div data-testid="panel">
      <button onClick={props.onClose}>close</button>
      <span data-testid="loading">{String(props.isLoading)}</span>
      <ul>
        {props.items.map((item) => (
          <li key={item.id}>
            <span>{item.label}</span>
            <span data-testid={`checked-${item.id}`}>
              {String(item.checked)}
            </span>
            <span data-testid={`locked-${item.id}`}>
              {String(!!item.locked)}
            </span>
            <button onClick={() => props.onToggle(item.id)}>
              toggle {item.id}
            </button>
          </li>
        ))}
      </ul>
    </div>
  ),
}));

const available = [{ name: 'agenda-widget' }, { name: 'carnet-de-bord' }].map(
  (widget) => ({ ...widget, catalog: true, Component: () => null }),
);

beforeEach(() => {
  mocks.translations = {};
  mocks.useHomepageWidgets.mockReturnValue({ available, displayed: [] });
});

describe('WidgetsPersonalizationPanelContainer', () => {
  it('maps each available widget to a row (label, checked, locked)', () => {
    mocks.useWidgetPreferences.mockReturnValue({
      isLoading: false,
      isVisible: (name: string) => name === 'agenda-widget',
      isLocked: (name: string) => name === 'carnet-de-bord',
      toggleWidget: vi.fn(),
    });

    render(<WidgetsPersonalizationPanelContainer onClose={vi.fn()} />);

    expect(screen.getByText('Agenda')).toBeInTheDocument();
    expect(screen.getByText('Carnet De Bord')).toBeInTheDocument();
    expect(screen.getByTestId('checked-agenda-widget')).toHaveTextContent(
      'true',
    );
    expect(screen.getByTestId('checked-carnet-de-bord')).toHaveTextContent(
      'false',
    );
    expect(screen.getByTestId('locked-carnet-de-bord')).toHaveTextContent(
      'true',
    );
    expect(screen.getByTestId('locked-agenda-widget')).toHaveTextContent(
      'false',
    );
  });

  it('calls toggleWidget with the widget name when a row toggle is clicked', async () => {
    const toggleWidget = vi.fn();
    mocks.useWidgetPreferences.mockReturnValue({
      isLoading: false,
      isVisible: () => false,
      isLocked: () => false,
      toggleWidget,
    });

    render(<WidgetsPersonalizationPanelContainer onClose={vi.fn()} />);
    screen.getByText('toggle agenda-widget').click();

    expect(toggleWidget).toHaveBeenCalledWith('agenda-widget');
  });

  it('prefers the label written for this panel over the legacy one', () => {
    mocks.translations = {
      'timeline.settings.agenda-widget': 'Legacy agenda',
      'timeline.widgets.agenda-widget': 'Panel agenda',
      'timeline.settings.carnet-de-bord': 'Legacy carnet',
    };
    mocks.useWidgetPreferences.mockReturnValue({
      isLoading: false,
      isVisible: () => true,
      isLocked: () => false,
      toggleWidget: vi.fn(),
    });

    render(<WidgetsPersonalizationPanelContainer onClose={vi.fn()} />);

    expect(screen.getByText('Panel agenda')).toBeInTheDocument();
    expect(screen.queryByText('Legacy agenda')).not.toBeInTheDocument();
    // No panel-specific label: the legacy one is used
    expect(screen.getByText('Legacy carnet')).toBeInTheDocument();
  });

  it('lists no row when no widget is available', () => {
    mocks.useHomepageWidgets.mockReturnValue({ available: [], displayed: [] });
    mocks.useWidgetPreferences.mockReturnValue({
      isLoading: false,
      isVisible: () => true,
      isLocked: () => false,
      toggleWidget: vi.fn(),
    });

    render(<WidgetsPersonalizationPanelContainer onClose={vi.fn()} />);

    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
  });

  it('forwards isLoading to the panel', () => {
    mocks.useWidgetPreferences.mockReturnValue({
      isLoading: true,
      isVisible: () => true,
      isLocked: () => false,
      toggleWidget: vi.fn(),
    });

    render(<WidgetsPersonalizationPanelContainer onClose={vi.fn()} />);

    expect(screen.getByTestId('loading')).toHaveTextContent('true');
  });

  it('calls onClose when the panel requests to close', () => {
    const onClose = vi.fn();
    mocks.useWidgetPreferences.mockReturnValue({
      isLoading: false,
      isVisible: () => true,
      isLocked: () => false,
      toggleWidget: vi.fn(),
    });

    render(<WidgetsPersonalizationPanelContainer onClose={onClose} />);
    screen.getByText('close').click();

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
