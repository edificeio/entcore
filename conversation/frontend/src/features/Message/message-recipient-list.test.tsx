import { mockFullMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageRecipientList } from './message-recipient-list';

const mockRecipients = mockFullMessage.to;

vi.mock("@edifice.io/react", () => ({
  useEdificeClient: vi.fn(),
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useEdificeClient: () => ({
      user: { userId: mockRecipients.users[0].id },
    }),
  };
});


describe('Message recipient list', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render a label successfully', async () => {
    const label = "To :"
    const { baseElement } = render(<MessageRecipientList label={label} recipients={mockRecipients} />);
    expect(baseElement).toBeTruthy();

    const recipientsLabel = await screen.findByText(label);
    expect(recipientsLabel).toBeInTheDocument();
  });

  it('should display "Me" instead of the first name when the user is the logged-in user', async () => {
    render(<MessageRecipientList label={"To"} recipients={mockRecipients} />);
    const currentUserLabel = await screen.findAllByText("me");
    expect(currentUserLabel.length).equal(1);
  });

  it('should open the userbook in a new tab when clicking on a user', async () => {
    const displayName = "GUEDON Aliénor"
    render(<MessageRecipientList label={"To"} recipients={mockRecipients} />);
    const link = screen.getByRole("link", { name: displayName });
    expect(link).toHaveAttribute("target", "_blank");

    // TODO check the link is the userbook url
  });


});
