import { render, screen, within } from '~/mocks/setup';
import { Root } from './index';

/**
 * Mock window.matchMedia used in useBreakpoint hook, and isolate Root's
 * layout logic from the real PageLayout / homepage containers implementation.
 */
const mocks = vi.hoisted(() => ({
  useBreakpoint: vi.fn(),
  updateOverlayOpen: vi.fn(),
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );

  const PageLayoutMock = ({ children }: { children: React.ReactNode }) => (
    <div data-testid="page-layout">{children}</div>
  );
  PageLayoutMock.Header = ({
    onNotificationsClick,
  }: {
    onNotificationsClick?: () => void;
  }) => (
    <div data-testid="page-header">
      <button onClick={onNotificationsClick}>Notifications</button>
    </div>
  );
  PageLayoutMock.SidebarLeft = ({
    children,
  }: {
    children: React.ReactNode;
  }) => <div data-testid="sidebar-left">{children}</div>;
  PageLayoutMock.Content = ({ children }: { children: React.ReactNode }) => (
    <div data-testid="content">{children}</div>
  );
  PageLayoutMock.SidebarRight = ({
    children,
  }: {
    children: React.ReactNode;
  }) => <div data-testid="sidebar-right">{children}</div>;
  PageLayoutMock.Overlay = ({ children }: { children: React.ReactNode }) => (
    <div data-testid="overlay">{children}</div>
  );
  PageLayoutMock.HelpZone = () => <div data-testid="help-zone" />;

  return {
    ...actual,
    useBreakpoint: mocks.useBreakpoint,
    useEdificeClient: () => ({ init: true }),
    useOverlay: () => ({ updateOverlayOpen: mocks.updateOverlayOpen }),
    PageLayout: PageLayoutMock,
    // Root doesn't exercise session bootstrap directly (useEdificeClient is
    // already mocked above) — stub the provider itself too so mounting it
    // via `~/providers` doesn't require a real session.
    EdificeClientProvider: ({ children }: { children: React.ReactNode }) => (
      <>{children}</>
    ),
  };
});

vi.mock('@edifice.io/react/homepage', () => ({
  MessageFlashListContainer: () => (
    <div data-testid="message-flash-list-container" />
  ),
  FavoritesContainer: () => <div data-testid="favorites-container" />,
  LastInfosContainer: () => <div data-testid="last-infos-container" />,
  NotificationListContainer: () => (
    <div data-testid="notification-list-container" />
  ),
  SchoolSpaceContainer: () => <div data-testid="school-space-container" />,
  UsefulLinksContainer: () => <div data-testid="useful-links-container" />,
  UserSpaceContainer: ({
    children,
    onCustomizeWidgetsClick,
  }: {
    children: React.ReactNode;
    onCustomizeWidgetsClick?: () => void;
  }) => (
    <div data-testid="user-space-container">
      {onCustomizeWidgetsClick && (
        <button onClick={onCustomizeWidgetsClick}>
          Personnaliser mes widgets
        </button>
      )}
      {children}
    </div>
  ),
}));

vi.mock('~/components/BetaSwitch/BetaSwitchContainer', () => ({
  BetaSwitchContainer: () => <div data-testid="beta-switch-container" />,
}));

vi.mock(
  '~/components/WidgetsPersonalizationPanel/WidgetsPersonalizationPanelContainer',
  () => ({
    WidgetsPersonalizationPanelContainer: ({
      onClose,
    }: {
      onClose: () => void;
    }) => (
      <div data-testid="widgets-personalization-panel">
        <button onClick={onClose}>Fermer le volet widgets</button>
      </div>
    ),
  }),
);

describe('Root - MessageFlashListContainer responsive placement', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders MessageFlashListContainer in the left sidebar on small/responsive screens (sm, not md)', () => {
    mocks.useBreakpoint.mockReturnValue({ sm: true, md: false });

    render(<Root />);

    const sidebarLeft = screen.getByTestId('sidebar-left');
    const content = screen.getByTestId('content');

    expect(
      within(sidebarLeft).getByTestId('message-flash-list-container'),
    ).toBeInTheDocument();
    expect(
      within(content).queryByTestId('message-flash-list-container'),
    ).not.toBeInTheDocument();
  });

  it('renders MessageFlashListContainer in the main content on desktop screens (md)', () => {
    mocks.useBreakpoint.mockReturnValue({ sm: true, md: true });

    render(<Root />);

    const sidebarLeft = screen.getByTestId('sidebar-left');
    const content = screen.getByTestId('content');

    expect(
      within(content).getByTestId('message-flash-list-container'),
    ).toBeInTheDocument();
    expect(
      within(sidebarLeft).queryByTestId('message-flash-list-container'),
    ).not.toBeInTheDocument();
  });
});

describe('Root - widgets personalization panel / notifications mutual exclusion', () => {
  beforeEach(() => {
    mocks.useBreakpoint.mockReturnValue({ sm: true, md: true });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('opens the widgets panel and hides notifications when the trigger is clicked', () => {
    render(<Root />);

    screen.getByRole('button', { name: 'Personnaliser mes widgets' }).click();

    const overlay = screen.getByTestId('overlay');
    expect(
      within(overlay).getByTestId('widgets-personalization-panel'),
    ).toBeInTheDocument();
    expect(
      within(overlay).queryByTestId('notification-list-container'),
    ).not.toBeInTheDocument();
    expect(mocks.updateOverlayOpen).toHaveBeenLastCalledWith(true);
  });

  it('closes the widgets panel and falls back to notifications when it requests to close', () => {
    render(<Root />);
    screen.getByRole('button', { name: 'Personnaliser mes widgets' }).click();

    screen.getByRole('button', { name: 'Fermer le volet widgets' }).click();

    const overlay = screen.getByTestId('overlay');
    expect(
      within(overlay).queryByTestId('widgets-personalization-panel'),
    ).not.toBeInTheDocument();
    expect(
      within(overlay).getByTestId('notification-list-container'),
    ).toBeInTheDocument();
    expect(mocks.updateOverlayOpen).toHaveBeenLastCalledWith(false);
  });

  it('closes the widgets panel when notifications are toggled open while it is showing', () => {
    render(<Root />);
    screen.getByRole('button', { name: 'Personnaliser mes widgets' }).click();
    expect(
      screen.getByTestId('widgets-personalization-panel'),
    ).toBeInTheDocument();

    screen.getByRole('button', { name: 'Notifications' }).click();

    const overlay = screen.getByTestId('overlay');
    expect(
      within(overlay).queryByTestId('widgets-personalization-panel'),
    ).not.toBeInTheDocument();
    expect(
      within(overlay).getByTestId('notification-list-container'),
    ).toBeInTheDocument();
  });
});
