import { render, renderHook, screen, waitFor, wrapper } from '~/mocks/setup';
import { FolderList } from './folder-list';
import { useFolderMessages } from '~/services';

/**
 * Mock useParams
 */
const mocks = vi.hoisted(() => ({
  useParams: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual =
    await vi.importActual<typeof import('react-router-dom')>(
      'react-router-dom',
    );
  return {
    ...actual,
    useParams: mocks.useParams,
  };
});

describe('Folder list component', () => {
  beforeEach(() => {
    mocks.useParams.mockReturnValue({ folderId: 'inbox' });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<FolderList />);

    expect(baseElement).toBeTruthy();
  });

  it('should render successfully a list of messages', async () => {
    const { result } = renderHook(() => useFolderMessages('inbox'), {
      wrapper: ({ children }: { children: React.ReactNode }) =>
        wrapper({
          initialEntries: ['/inbox'],
          children,
        }),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    render(<FolderList />, { path: '/inbox' });

    const messages = await screen.queryAllByTestId('message-item');
    expect(messages).toHaveLength(2);
  });
});
