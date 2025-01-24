import { render, screen } from '~/mocks/setup';
import { FolderList } from './folder-list';
import { folderService } from '~/services';
import { renderWithRouter } from '~/mocks/renderWithRouter';

describe('Folder header component', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<FolderList />);

    expect(baseElement).toBeTruthy();
  });

  it('should render successfully', async () => {
    const folderServiceSpy = vi.spyOn(folderService, 'getMessages');
    renderWithRouter('/inbox', <FolderList />);

    expect(folderServiceSpy).toHaveBeenCalled();
    const messages = await screen.queryAllByTestId('message-item');
    expect(messages).toHaveLength(1);
  });
});
