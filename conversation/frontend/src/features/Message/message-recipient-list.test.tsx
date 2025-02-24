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

  it('should render successfully', async () => {
    const { baseElement } = render(<MessageRecipientList label={"To :"} recipients={mockRecipients} />);
    expect(baseElement).toBeTruthy();
  });

  it('should render a label ', async () => {
    const label = "To :"
    render(<MessageRecipientList label={label} recipients={mockRecipients} />);
    const recipientsLabel = await screen.findByText(label);
    expect(recipientsLabel).toBeInTheDocument();
  });

  it('should display "Me" instead of the first name when the user is the logged-in user', async () => {
    render(<MessageRecipientList label={"To"} recipients={mockRecipients} />);
    const currentUserLabel = await screen.findAllByText("me");
    expect(currentUserLabel.length).equal(1);
  });

  it('should open the userbook in a new tab when clicking on a user', async () => {
    render(<MessageRecipientList label={"To"} recipients={mockRecipients} />);

    const displayNameUser = "GUEDON Aliénor"
    const linkUser = screen.getByRole("link", { name: displayNameUser });
    const linkUserUrl = linkUser.getAttribute("href");

    expect(linkUserUrl).toContain("/userbook/annuaire");
    expect(linkUser).toHaveAttribute("target", "_blank");
  });

  it('should open the userbook in a new tab when clicking on a group', async () => {
    render(<MessageRecipientList label={"To"} recipients={mockRecipients} />);

    const displayNameGroup = "Enseignants du groupe scolaire."
    const linkGroup = screen.getByRole("link", { name: displayNameGroup });
    const linkGroupUrl = linkGroup.getAttribute("href");

    expect(linkGroupUrl).toContain("/userbook/annuaire#/group-view");
    expect(linkGroup).toHaveAttribute("target", "_blank");
  });


});
