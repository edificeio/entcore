import { fireEvent, render, screen } from '~/mocks/setup';
import { MessageListHeader } from './message-list-header';

/**
 * Mock window.matchMedia used in useBreakpoint hook
 */
const mocks = vi.hoisted(() => ({
  useBreakpoint: vi.fn(),
  useEdificeTheme: vi.fn(),
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useBreakpoint: mocks.useBreakpoint,
    useEdificeTheme: mocks.useEdificeTheme,
  };
});

describe('Message list header component', () => {
  beforeAll(() => {
    mocks.useBreakpoint.mockReturnValue({ md: true });
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: false } });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<MessageListHeader />);

    expect(baseElement).toBeTruthy();
  });

  it('should not change searchparams when search input change', async () => {
    const setSearchParamsSpy = vi.spyOn(URLSearchParams.prototype, 'set');

    render(<MessageListHeader />);
    const searchInput = await screen.findByTestId('search-bar');
    fireEvent.change(searchInput, { target: { value: 'test' } });

    expect(setSearchParamsSpy).not.toHaveBeenCalled();
  });

  it('should change searchparams when search input change and click', async () => {
    const setSearchParamsSpy = vi.spyOn(URLSearchParams.prototype, 'set');

    render(<MessageListHeader />);

    const searchInput = await screen.findByTestId('search-bar');
    fireEvent.change(searchInput, { target: { value: 'test' } });
    const searchButton = await screen.findByLabelText('search');
    fireEvent.click(searchButton);

    expect(setSearchParamsSpy).toHaveBeenCalled();
  });

  it('should display filter dropdown with 2D theme', async () => {
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: false } });

    render(<MessageListHeader />);

    const searchInput = await screen.findByText('filter');
    expect(searchInput).toBeInTheDocument();
  });

  it('should not display filter dropdown with 1D theme', async () => {
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: true } });

    render(<MessageListHeader />);

    const searchInput = await screen.queryByText('filter');
    expect(searchInput).not.toBeInTheDocument();
  });
});
