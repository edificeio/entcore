import { mockFullMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageRecipientSubList } from './MessageRecipientSubList';

const mockRecipients = mockFullMessage.to;
const RecipientListlabel = 'To :';

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

  it('should render a label ', async () => {
    render(
      <MessageRecipientSubList
        head={RecipientListlabel}
        recipients={mockRecipients}
      />,
    );
    const recipientsLabel = await screen.findByText(RecipientListlabel);
    expect(recipientsLabel).toBeInTheDocument();
  });

  it('should display "Me" instead of the first name when the user is the logged-in user', async () => {
    render(
      <MessageRecipientSubList
        head={RecipientListlabel}
        recipients={mockRecipients}
      />,
    );
    const currentUserLabel = await screen.findAllByText('me');
    expect(currentUserLabel.length).equal(1);
  });

  it('should open the userbook in a new tab when clicking on a user', async () => {
    render(
      <MessageRecipientSubList
        head={RecipientListlabel}
        recipients={mockRecipients}
      />,
    );
    const displayNameUser = 'ISABELLE POLONIO (prof arts plastiques)';
    const linkUser = screen.getByRole('link', { name: displayNameUser });
    const linkUserUrl = linkUser.getAttribute('href');

    expect(linkUserUrl).toContain('/userbook/annuaire');
    expect(linkUser).toHaveAttribute('target', '_blank');
  });

  it('should open the userbook in a new tab when clicking on a group', async () => {
    render(
      <MessageRecipientSubList
        head={RecipientListlabel}
        recipients={mockRecipients}
      />,
    );
    const displayNameGroup = 'Enseignants du groupe scolaire.';
    const linkGroup = screen.getByRole('link', { name: displayNameGroup });
    const linkGroupUrl = linkGroup.getAttribute('href');

    expect(linkGroupUrl).toContain('/userbook/annuaire#/group-view');
    expect(linkGroup).toHaveAttribute('target', '_blank');
  });
});
