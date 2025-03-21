import {
  mockCurrentUserPreview,
  mockMessageFromMeToMe,
  mockMessageOfOutbox,
  mockMessagesOfInbox,
} from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageMetadata } from '~/models';
import { MessagePreview } from './MessagePreview';

const inboxMessage = mockMessagesOfInbox[0];
const userFolderId = '23785dbc-dc2e-4f66-95a4-23f587d65008';

const mocks = vi.hoisted(() => ({
  useParams: vi.fn(),
  useEdificeTheme: vi.fn(),
  useEdificeClient: vi.fn(),
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

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useEdificeClient: mocks.useEdificeClient,
    useEdificeTheme: mocks.useEdificeTheme,
  };
});

describe('Message preview header component', () => {
  beforeAll(() => {
    mocks.useEdificeTheme.mockReturnValue({ theme: { is1d: false } });
    mocks.useEdificeClient.mockReturnValue({
      user: { userId: mockCurrentUserPreview.id },
    });
    mocks.useParams.mockReturnValue({ folderId: 'inbox' });
  });

  afterAll(() => {
    vi.clearAllMocks();
  });

  it('should render successfully', async () => {
    const { baseElement } = render(<MessagePreview message={inboxMessage} />);

    expect(baseElement).toBeTruthy();
  });

  it('should render successfully', async () => {
    render(<MessagePreview message={inboxMessage} />);

    const messagePreview = await screen.findByText(
      inboxMessage.from.displayName,
    );
    const messageSubject = await screen.findByText(inboxMessage.subject);
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
    render(<MessagePreview message={inboxMessage} />);

    const messagePreview = await screen.findByText(
      inboxMessage.from.displayName,
    );
    const messageSubject = await screen.findByText(inboxMessage.subject);
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
      ...inboxMessage,
      hasAttachment: true,
    };
    render(<MessagePreview message={messageWithAttachment} />);

    const messagePreview = await screen.findByText(
      inboxMessage.from.displayName,
    );
    const messageSubject = await screen.findByText(inboxMessage.subject);
    const messageResponse = screen.queryByTitle('message.replied');
    const messageHasAttachements = screen.queryByTitle(
      'message.has.attachment',
    );

    expect(messagePreview).toBeInTheDocument();
    expect(messageSubject).toBeInTheDocument();
    expect(messageResponse).not.toBeInTheDocument();
    expect(messageHasAttachements).toBeInTheDocument();
  });

  it('should display message hasAttachement successfully', async () => {
    const messageWithResponse: MessageMetadata = {
      ...inboxMessage,
      response: true,
    };
    render(<MessagePreview message={messageWithResponse} />);

    const messagePreview = await screen.findByText(
      inboxMessage.from.displayName,
    );
    const messageSubject = await screen.findByText(inboxMessage.subject);
    const messageResponse = screen.queryByTitle('message.replied');
    const messageHasAttachements = screen.queryByTitle(
      'message.has.attachment',
    );

    expect(messagePreview).toBeInTheDocument();
    expect(messageSubject).toBeInTheDocument();
    expect(messageResponse).toBeInTheDocument();
    expect(messageHasAttachements).not.toBeInTheDocument();
  });

  it('should display "to" label and recipient name when in outbox', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'outbox' });

    render(<MessagePreview message={inboxMessage} />);

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
    render(<MessagePreview message={inboxMessage} />);

    expect(inboxMessage.to.groups.length).toBeGreaterThan(1);

    const recipientAvatar = screen.getByLabelText('recipient.avatar.group');
    expect(recipientAvatar).toBeInTheDocument();
  });

  it('should display all recipients after "to" label when in outbox', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'outbox' });
    render(<MessagePreview message={mockMessageOfOutbox} />);

    const atLabel = await screen.findByText('at');
    const cciLabel = await screen.findByText('cci');
    const ccLabel = await screen.findByText('cc');
    const recipientItems = screen.getAllByRole('listitem');

    expect(atLabel).toBeInTheDocument();
    expect(ccLabel).toBeInTheDocument();
    expect(cciLabel).toBeInTheDocument();
    expect(recipientItems).toHaveLength(4);
  });

  it('should display a "draft" label when in draft', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'draft' });
    render(<MessagePreview message={mockMessageOfOutbox} />);
    screen.getByText('draft');
  });

  it('should not display any recipients if there are none when in draft', async () => {
    mocks.useParams.mockReturnValue({ folderId: 'draft' });
    const message = { ...mockMessageOfOutbox };
    message.to = { users: [], groups: [] };
    message.cc = { users: [], groups: [] };
    message.cci = { users: [], groups: [] };

    render(<MessagePreview message={message} />);

    const recipientItems = screen.queryAllByRole('listitem');
    expect(recipientItems).toHaveLength(0);
  });

  it('should display inbox icon when message come from inbox in user folder', async () => {
    mocks.useParams.mockReturnValue({
      folderId: userFolderId,
    });
    render(<MessagePreview message={inboxMessage} />);
    await screen.findByTitle('mail-in');
  });

  it('should display outbox icon when message come from inbox in user folder', async () => {
    mocks.useParams.mockReturnValue({
      folderId: userFolderId,
    });
    render(<MessagePreview message={mockMessageOfOutbox} />);
    await screen.findByTitle('mail-out');
  });

  it('should display inbox icon when current user send the message to himself in user folder', async () => {
    mocks.useParams.mockReturnValue({
      folderId: userFolderId,
    });

    render(<MessagePreview message={mockMessageFromMeToMe} />);

    await screen.findByTitle('mail-in');
  });
});
