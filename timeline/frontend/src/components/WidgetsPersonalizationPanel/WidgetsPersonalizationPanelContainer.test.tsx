import type { IWidgetModel } from '@edifice.io/client';
import { render, screen } from '@testing-library/react';
import type { WidgetsPersonalizationPanelProps } from '@edifice.io/react/homepage';
import { WidgetsPersonalizationPanelContainer } from './WidgetsPersonalizationPanelContainer';

const mocks = vi.hoisted(() => ({ useWidgetPreferences: vi.fn() }));

vi.mock('~/hooks/useWidgetPreferences', () => ({
  useWidgetPreferences: mocks.useWidgetPreferences,
}));

vi.mock('~/hooks/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, opts?: Record<string, unknown> | string) =>
      typeof opts === 'string' ? opts : ((opts?.defaultValue as string) ?? key),
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

// `i18n` mirrors the real API: a resource path, not a translation key —
// the container falls back to a label derived from `name` (see
// WidgetsPersonalizationPanelContainer's `fallbackLabel`).
const widget = (overrides: Partial<IWidgetModel>): IWidgetModel => ({
  id: 'w',
  js: '',
  path: '',
  i18n: '/assets/widgets/agenda-widget/i18n',
  mandatory: false,
  name: 'agenda-widget',
  ...overrides,
});

const widgets: IWidgetModel[] = [
  widget({ name: 'agenda-widget', i18n: '/assets/widgets/agenda-widget/i18n' }),
  widget({
    name: 'carnet-de-bord',
    i18n: '/assets/widgets/carnet-de-bord/i18n',
    mandatory: true,
  }),
];

describe('WidgetsPersonalizationPanelContainer', () => {
  it('maps each catalog widget to a row (label, checked, locked)', () => {
    mocks.useWidgetPreferences.mockReturnValue({
      widgets,
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
      widgets,
      isLoading: false,
      isVisible: () => false,
      isLocked: () => false,
      toggleWidget,
    });

    render(<WidgetsPersonalizationPanelContainer onClose={vi.fn()} />);
    screen.getByText('toggle agenda-widget').click();

    expect(toggleWidget).toHaveBeenCalledWith('agenda-widget');
  });

  it('forwards isLoading to the panel', () => {
    mocks.useWidgetPreferences.mockReturnValue({
      widgets,
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
      widgets: [],
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
