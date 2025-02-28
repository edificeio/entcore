import { mockFullMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageHeader } from './message-header';

describe('Message recipient list', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should display recipient list for To, Cc and Cci', async () => {
    render(<MessageHeader message={mockFullMessage} />);

    const toLabel = screen.queryByText('at');
    expect(toLabel).toBeInTheDocument();

    const ccLabel = screen.queryByText('cc');
    expect(ccLabel).toBeInTheDocument();

    const cciLabel = screen.queryByText('cci');
    expect(cciLabel).toBeInTheDocument();
  });

  it('should display recipient list for To but not Cc and Cci', async () => {
    mockFullMessage.cc = { users: [], groups: [] };
    mockFullMessage.cci = { users: [], groups: [] };
    render(<MessageHeader message={mockFullMessage} />);

    const toLabel = screen.queryByText('at');
    expect(toLabel).toBeInTheDocument();

    const ccLabel = screen.queryByText('cc');
    expect(ccLabel).not.toBeInTheDocument();

    const cciLabel = screen.queryByText('cci');
    expect(cciLabel).not.toBeInTheDocument();
  });
});
