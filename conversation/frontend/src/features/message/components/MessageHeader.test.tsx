import { mockFullMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageHeader } from './MessageHeader';

const mocks = vi.hoisted(() => ({
  useEdificeTheme: vi.fn(),
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useEdificeTheme: mocks.useEdificeTheme,
  };
});

describe('Message recipient list', () => {
  beforeAll(() => {
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: false } });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should display recipient list for To, Cc', async () => {
    render(<MessageHeader message={mockFullMessage} />);

    const toLabel = screen.queryByText('at');
    expect(toLabel).toBeInTheDocument();

    const ccLabel = screen.queryByText('cc');
    expect(ccLabel).toBeInTheDocument();
  });

  it('should display recipient list for To but not Cc and Cci', async () => {
    const message = { ...mockFullMessage };
    message.cc = { users: [], groups: [] };
    render(<MessageHeader message={message} />);

    const toLabel = screen.queryByText('at');
    expect(toLabel).toBeInTheDocument();

    const ccLabel = screen.queryByText('cc');
    expect(ccLabel).not.toBeInTheDocument();
  });

  it('should display sender displayStructure name when defined', async () => {
    render(<MessageHeader message={mockFullMessage} />);

    const structureLabel = await screen.findByText('Collège Jean Moulin');
    expect(structureLabel).toBeInTheDocument();
    expect(structureLabel.tagName).toBe('SPAN');
  });

  it('should not display any structure label when displayStructure is absent', async () => {
    const message = {
      ...mockFullMessage,
      from: { ...mockFullMessage.from!, displayStructure: undefined },
    };
    render(<MessageHeader message={message} />);

    await screen.findByText(mockFullMessage.from!.displayName);
    expect(screen.queryByText('Collège Jean Moulin')).not.toBeInTheDocument();
  });
});
