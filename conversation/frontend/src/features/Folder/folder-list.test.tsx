import { render } from '~/mocks/setup';
import { FolderList } from './folder-list';

describe('Folder header component', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<FolderList />);

    expect(baseElement).toBeTruthy();
  });
  // it('should render successfully', async () => {
  //   const folderServiceSpy = vi.spyOn(folderService, 'getMessages');
  //   renderWithRouter('/inbox', <FolderList />);
    
  //   // const messages = await screen.findAllByTestId('message-item');
  //   // expect(messages).toHaveLength(1);
  //   expect(folderServiceSpy).toHaveBeenCalled();
  // });

});
