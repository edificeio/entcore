import { act } from 'react';
import { render, renderHook, screen, waitFor, wrapper } from '~/mocks/setup';
import { useFolderMessages } from '~/services';
import { MessageList } from './MessageList';

/**
 * Mock useParams
 */
const mocks = vi.hoisted(() => ({
  useSelectedFolder: vi.fn(),
}));

const mockIntersectionObserver = vi.fn().mockReturnValue({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
});

vi.mock('~/hooks/useSelectedFolder', () => ({
  useSelectedFolder: mocks.useSelectedFolder,
}));

describe('Message list component', () => {
  beforeAll(() => {
    mocks.useSelectedFolder.mockReturnValue({ folderId: 'inbox' });
    window.IntersectionObserver = mockIntersectionObserver;
  });

  afterAll(() => {
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
      checkboxList[1].click();
      checkboxList[2].click();
      checkboxList[3].click();
    });

    expect(screen.queryByLabelText('tag.read')).toBeVisible();
    expect(screen.queryByLabelText('tag.unread')).toBeVisible();
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

    checkboxList[2].click();
    checkboxList[1].click();
    checkboxList[3].click();
    checkboxList[4].click();

    await waitFor(() => {
      expect(screen.queryByLabelText('tag.read')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('tag.unread')).not.toBeInTheDocument();
    });

    checkboxList[4].click();

    await waitFor(() => {
      expect(screen.queryByLabelText('tag.read')).toBeInTheDocument();
      expect(screen.queryByLabelText('tag.unread')).toBeInTheDocument();
    });

    checkboxList[0].click();

    await waitFor(() => {
      expect(screen.queryByLabelText('tag.read')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('tag.unread')).not.toBeInTheDocument();
    });
  });
});
