import { render, screen, within } from '~/mocks/setup';
import { Root } from './index';

/**
 * Mock window.matchMedia used in useBreakpoint hook, and isolate Root's
 * layout logic from the real PageLayout / homepage containers implementation.
 */
const mocks = vi.hoisted(() => ({
  useBreakpoint: vi.fn(),
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );

  const PageLayoutMock = ({ children }: { children: React.ReactNode }) => (
    <div data-testid="page-layout">{children}</div>
  );
  PageLayoutMock.Header = () => <div data-testid="page-header" />;
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

  return {
    ...actual,
    useBreakpoint: mocks.useBreakpoint,
    useEdificeClient: () => ({ init: true }),
    useOverlay: () => ({ updateOverlayOpen: vi.fn() }),
    PageLayout: PageLayoutMock,
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
  UserSpaceContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="user-space-container">{children}</div>
  ),
}));

vi.mock('~/components/BetaSwitch/BetaSwitchContainer', () => ({
  BetaSwitchContainer: () => <div data-testid="beta-switch-container" />,
}));

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
