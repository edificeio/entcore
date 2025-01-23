import { fireEvent, render, screen } from '~/mocks/setup';
import { FolderHeader } from './folder-header';
import { folderService } from '~/services';

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

describe('Folder header component', () => {
  beforeAll(() => {
    mocks.useBreakpoint.mockReturnValue({ md: true });
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: false } });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<FolderHeader />);

    expect(baseElement).toBeTruthy();
  });

  it('should handle search input change and click', async () => {
    const folderServiceSpy = vi.spyOn(folderService, 'getMessages');

    render(<FolderHeader />);
    const searchInput = await screen.findByTestId('search-bar');
    fireEvent.change(searchInput, { target: { value: 'test' } });
    expect(folderServiceSpy).not.toHaveBeenCalled();
  });

  it('should display filter dropdown', async () => {
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: false } });

    render(<FolderHeader />);
    const searchInput = await screen.findByText('filter');
    expect(searchInput).toBeInTheDocument();
  });

  it('should not display filter dropdown', async () => {
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: true } });

    render(<FolderHeader />);
    const searchInput = await screen.queryByText('filter');
    expect(searchInput).not.toBeInTheDocument();
  });
});
