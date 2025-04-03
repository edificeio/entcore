import { act } from 'react';
import { render, renderHook, screen, waitFor, wrapper } from '~/mocks/setup';
import { useFolderMessages } from '~/services';
import { MessageList } from './MessageList';

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

describe('Message list component', () => {
  beforeEach(() => {
    mocks.useParams.mockReturnValue({ folderId: 'inbox' });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<MessageList />);

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

    render(<MessageList />, { path: '/inbox' });

    const messages = screen.queryAllByTestId('message-item');
    expect(messages).toHaveLength(4);
  });

  it('should render mark as read / unread action for a list of selected messages', async () => {
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

    render(<MessageList />, { path: '/inbox' });

    const checkboxList = screen.getAllByRole('checkbox');
    act(() => {
      checkboxList[2].click();
      checkboxList[1].click();
      checkboxList[3].click();
    });

    expect(screen.getByText('tag.read')).toBeVisible();
    expect(screen.getByText('tag.unread')).toBeVisible();
  });

  it('should not render mark as read / unread action when selected message is from current user without being a recipient', async () => {
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

    render(<MessageList />, { path: '/inbox' });

    const checkboxList = screen.getAllByRole('checkbox');
    act(() => {
      checkboxList[2].click();
      checkboxList[1].click();
      checkboxList[3].click();
      checkboxList[4].click();
    });

    expect(screen.getByText('tag.read')).not.toBeVisible();
    expect(screen.getByText('tag.unread')).not.toBeVisible();
    act(() => {
      checkboxList[4].click();
    });
    expect(screen.getByText('tag.read')).toBeVisible();
    expect(screen.getByText('tag.unread')).toBeVisible();
    act(() => {
      checkboxList[0].click();
    });
    expect(screen.getByText('tag.read')).not.toBeVisible();
    expect(screen.getByText('tag.unread')).not.toBeVisible();
  });
});
