import { render, screen } from '~/mocks/setup';
import { MessageListEmpty } from './message-list-empty';

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

describe('Message list empty component', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it.each([
    {
      folderId: 'inbox',
      key: 'messagerie',
      illuSrc: 'illu-messagerie.svg',
      path: '/inbox',
      withNewMessage: true,
    },
    {
      folderId: 'outbox',
      key: 'messagerie',
      illuSrc: 'illu-messagerie.svg',
      path: '/outbox',
      withNewMessage: true,
    },
    {
      folderId: 'draft',
      key: 'messagerie',
      illuSrc: 'illu-messagerie.svg',
      path: '/draft',
      withNewMessage: true,
    },
    {
      folderId: 'inbox',
      key: 'search',
      illuSrc: 'illu-search.svg',
      path: '/inbox?search=Edifice',
      withNewMessage: false,
    },
    {
      folderId: 'trash',
      key: 'trash',
      illuSrc: 'illu-trash.svg',
      path: '/trash',
      withNewMessage: false,
    },
    {
      folderId: 'userFolder',
      key: 'noContent',
      illuSrc: 'illu-no-content-in-folder.svg',
      path: '/userFolder',
      withNewMessage: false,
    },
  ])(
    'should render successfully empty state for $key',
    async (folderEmptyData: {
      folderId: string;
      key: string;
      illuSrc: string;
      path: string;
      withNewMessage: boolean;
    }) => {
      mocks.useParams.mockReturnValue({ folderId: folderEmptyData.folderId });

      render(<MessageListEmpty />, { path: folderEmptyData.path });

      const image = await screen.queryByAltText(
        `${folderEmptyData.key}.empty.title`,
      );
      const title = await screen.queryByText(
        `${folderEmptyData.key}.empty.title`,
      );
      const text = await screen.queryByText(
        `${folderEmptyData.key}.empty.text`,
      );
      const newMessageButton = await screen.queryByText(`new.message`);

      expect(image).toBeInTheDocument();
      expect(image).toHaveAttribute(
        'src',
        expect.stringContaining(`${folderEmptyData.illuSrc}`),
      );
      expect(title).toBeInTheDocument();
      expect(text).toBeInTheDocument();

      if (folderEmptyData.withNewMessage) {
        expect(newMessageButton).toBeInTheDocument();
      } else {
        expect(newMessageButton).not.toBeInTheDocument();
      }
    },
  );
});
