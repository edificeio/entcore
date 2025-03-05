import { mockMessagesOfInbox } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageMetadata } from '~/models';
import { MessagePreview } from './message-preview';

const message = mockMessagesOfInbox[0];

describe('Message preview header component', () => {
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
    const messageResponse = await screen.queryByTestId('message-response');
    const messageHasAttachements = await screen.queryByTestId(
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
    const messageResponse = await screen.queryByTitle('message-response');
    const messageHasAttachements = await screen.queryByTitle(
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
    const messageResponse = await screen.queryByTitle('message-response');
    const messageHasAttachements = await screen.queryByTitle(
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
    const messageResponse = await screen.queryByTitle('message-response');
    const messageHasAttachements = await screen.queryByTitle(
      'message-has-attachment',
    );

    expect(messagePreview).toBeInTheDocument();
    expect(messageSubject).toBeInTheDocument();
    expect(messageResponse).toBeInTheDocument();
    expect(messageHasAttachements).not.toBeInTheDocument();
  });
});
