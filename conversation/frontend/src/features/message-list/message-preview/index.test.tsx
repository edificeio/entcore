import { mockMessageOfOutbox, mockMessagesOfInbox } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageMetadata } from '~/models';
import { MessagePreview } from '.';

const message = mockMessagesOfInbox[0];

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

describe('Message preview header component', () => {
  beforeEach(() => {
    mocks.useParams.mockReturnValue({ folderId: 'inbox' });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<MessagePreview message={message} />);

    expect(baseElement).toBeTruthy();
  });

  it('should render successfully', async () => {
    render(<MessagePreview message={message} />);

    const messagePreview = await screen.findByText(message.from.displayName);
    const messageSubject = await screen.findByText(message.subject);
    const messageResponse = screen.queryByTestId('message-response');
    const messageHasAttachements = screen.queryByTestId(
      'message-has-attachment',
    );

    expect(messagePreview).toBeInTheDocument();
    expect(messageSubject).toBeInTheDocument();
    expect(messageResponse).not.toBeInTheDocument();
    expect(messageHasAttachements).not.toBeInTheDocument();
  });

  it('should display message informations successfully', async () => {
    render(<MessagePreview message={message} />);

    const messagePreview = await screen.findByText(message.from.displayName);
    const messageSubject = await screen.findByText(message.subject);
    const messageResponse = screen.queryByTitle('message-response');
    const messageHasAttachements = screen.queryByTitle(
      'message-has-attachment',
    );

    expect(messagePreview).toBeInTheDocument();
    expect(messageSubject).toBeInTheDocument();
    expect(messageResponse).not.toBeInTheDocument();
    expect(messageHasAttachements).not.toBeInTheDocument();
  });

  it('should display message hasAttachement successfully', async () => {
    const messageWithAttachment: MessageMetadata = {
      ...message,
      hasAttachment: true,
    };
    render(<MessagePreview message={messageWithAttachment} />);

    const messagePreview = await screen.findByText(message.from.displayName);
    const messageSubject = await screen.findByText(message.subject);
    const messageResponse = screen.queryByTitle('message-response');
    const messageHasAttachements = screen.queryByTitle(
      'message-has-attachment',
    );

    expect(messagePreview).toBeInTheDocument();
    expect(messageSubject).toBeInTheDocument();
    expect(messageResponse).not.toBeInTheDocument();
    expect(messageHasAttachements).toBeInTheDocument();
  });

  it('should display message hasAttachement successfully', async () => {
    const messageWithResponse: MessageMetadata = {
      ...message,
      response: true,
    };
    render(<MessagePreview message={messageWithResponse} />);

    const messagePreview = await screen.findByText(message.from.displayName);
    const messageSubject = await screen.findByText(message.subject);
    const messageResponse = screen.queryByTitle('message-response');
    const messageHasAttachements = screen.queryByTitle(
      'message-has-attachment',
    );

    expect(messagePreview).toBeInTheDocument();
    expect(messageSubject).toBeInTheDocument();
    expect(messageResponse).toBeInTheDocument();
    expect(messageHasAttachements).not.toBeInTheDocument();
  });

  it('should display "to" label and recipient name when in outbox', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'outbox' });

    render(<MessagePreview message={message} />);

    const toLabel = screen.getByText('at');
    expect(toLabel).toBeInTheDocument();

    const senderName = screen.getByText('Enseignants du groupe scolaire.');
    expect(senderName).toBeInTheDocument();
  });

  it('should display the recipient avatar when in outbox', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'outbox' });

    const messageWithOneRecipient = mockMessagesOfInbox[1];
    render(<MessagePreview message={messageWithOneRecipient} />);

    expect(messageWithOneRecipient.to.groups.length).equal(1);

    const recipientAvatar = screen.getByAltText('recipient.avatar');
    expect(recipientAvatar).toBeInTheDocument();
  });

  it('should display group avatar icon when more than one recipient when in outbox', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'outbox' });
    render(<MessagePreview message={message} />);

    expect(message.to.groups.length).toBeGreaterThan(1);

    const recipientAvatar = screen.getByLabelText('recipient.avatar.group');
    expect(recipientAvatar).toBeInTheDocument();
  });

  it('should display all recipients after "to" label when in outbox', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'outbox' });
    render(<MessagePreview message={mockMessageOfOutbox} />);

    screen.getByText('at');

    const recipientItems = screen.getAllByRole('listitem');
    expect(recipientItems).toHaveLength(5);
  });

  it('should display a "draft" label when in draft', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'draft' });
    render(<MessagePreview message={mockMessageOfOutbox} />);
    screen.getByText('draft');
  });

  it('should display all recipients without "to" label when in draft', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'draft' });
    render(<MessagePreview message={mockMessageOfOutbox} />);

    const atElement = screen.queryByText('at');
    expect(atElement).toBeNull();

    const recipientItems = screen.queryAllByRole('listitem');
    expect(recipientItems).toHaveLength(5);
  });

  it.only('should not display any recipients if there are none when in draft', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'draft' });
    const message = { ...mockMessageOfOutbox };
    message.to = { users: [], groups: [] };
    message.cc = { users: [], groups: [] };
    message.cci = { users: [], groups: [] };

    render(<MessagePreview message={message} />);

    const recipientItems = screen.queryAllByRole('listitem');
    expect(recipientItems).toHaveLength(0);
  });
});
