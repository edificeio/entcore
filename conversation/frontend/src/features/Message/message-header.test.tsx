import { mockFullMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageHeader } from './message-header';


describe('Message recipient list', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<MessageHeader message={mockFullMessage} />);
    expect(baseElement).toBeTruthy();
  });

  it('should display recipient list for To, Cc and Cci', async () => {
    render(<MessageHeader message={mockFullMessage} />);

    const toLabel = await screen.findByText("at");
    const ccLabel = await screen.findByText("cc");
    const cciLabel = await screen.findByText("cci");

    expect(toLabel).toBeInTheDocument();
    expect(ccLabel).toBeInTheDocument();
    expect(cciLabel).toBeInTheDocument();
  });

  it('should display recipient list for To but not Cc and Cci', async () => {
    mockFullMessage.cc = { users: [], groups: [] };
    mockFullMessage.cci = { users: [], groups: [] };
    render(<MessageHeader message={mockFullMessage} />);

    const toLabel = await screen.findByText("at");
    const ccLabel = await screen.queryByText("cc");
    const cciLabel = await screen.queryByText("cci");

    expect(toLabel).toBeInTheDocument();
    expect(ccLabel).not.toBeInTheDocument();
    expect(cciLabel).not.toBeInTheDocument();
  });

});
