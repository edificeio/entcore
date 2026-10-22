import { Visible, VisibleType } from '~/models/visible';
import { render, screen, waitFor } from '~/mocks/setup';
import { RecipientListEdit } from './RecipientListEdit';

const favorite: Visible = {
  id: 'favorite-1',
  displayName: 'Favorite group',
  type: VisibleType.ShareBookmark,
  usedIn: ['TO', 'CC', 'CCI'],
};

const searchedUser: Visible = {
  id: 'user-1',
  displayName: 'Marie Curie',
  profile: 'Teacher',
  type: VisibleType.User,
  usedIn: ['TO', 'CC', 'CCI'],
};

const mocks = vi.hoisted(() => ({
  useEdificeTheme: vi.fn(),
  useEdificeClient: vi.fn(),
  searchVisible: vi.fn(),
  // Stable reference: a new array on each render would loop on the bookmarks effect.
  defaultBookmarks: { data: [] as Visible[] },
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useEdificeTheme: mocks.useEdificeTheme,
    useEdificeClient: mocks.useEdificeClient,
    useIsAdml: () => ({ isAdml: false }),
  };
});

vi.mock('~/services/queries/user', async () => {
  const actual = await vi.importActual<
    typeof import('~/services/queries/user')
  >('~/services/queries/user');
  return {
    ...actual,
    useDefaultBookmark: () => mocks.defaultBookmarks,
    useSearchVisible: () => ({ searchVisible: mocks.searchVisible }),
    useBookmarkById: () => ({ getBookmarkById: vi.fn() }),
  };
});

const renderRecipientListEdit = () =>
  render(
    <RecipientListEdit
      recipients={{ users: [], groups: [] }}
      head={<span>at</span>}
      recipientType="to"
    />,
  );

describe('RecipientListEdit', () => {
  beforeAll(() => {
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: false } });
    mocks.useEdificeClient.mockReturnValue({
      user: { userId: 'current-user' },
    });
    mocks.defaultBookmarks.data = [favorite];
  });

  beforeEach(() => {
    mocks.searchVisible.mockImplementation(async (search: string) =>
      search ? [searchedUser] : [],
    );
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should keep and select the search text after selecting a recipient', async () => {
    const { user } = renderRecipientListEdit();
    const input = screen.getByTestId<HTMLInputElement>('combobox-search-input');

    await user.type(input, 'mar');
    await user.click(await screen.findByText(/Marie Curie/));

    await waitFor(() => expect(input.selectionEnd).toBe(3));
    expect(input).toHaveValue('mar');
    expect(input.selectionStart).toBe(0);
  });

  it('should show the favorites again when the input was emptied by the list closing', async () => {
    const { user } = renderRecipientListEdit();
    const input = screen.getByTestId<HTMLInputElement>('combobox-search-input');

    await user.type(input, 'mar');
    await screen.findByText(/Marie Curie/);

    // Reproduce what the Combobox does when its list closes: it empties the
    // input in the DOM, without any change event.
    input.blur();
    input.value = '';
    await user.click(input);

    expect(
      await screen.findByText('conversation.users.search.favorites'),
    ).toBeInTheDocument();
    expect(screen.getByText(/Favorite group/)).toBeInTheDocument();
    expect(screen.queryByText(/Marie Curie/)).not.toBeInTheDocument();
  });
});
