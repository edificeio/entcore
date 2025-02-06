import { mockFullMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageAttachments } from './message-attachments';

const message = mockFullMessage;

describe('Message preview header component', () => {
  it('should render successfully', async () => {
    const { baseElement } = render(
      <MessageAttachments
        messageId={message.id}
        attachments={message.attachments}
      />,
    );

    expect(baseElement).toBeTruthy();
  });

  it('should render multiple attachements successfully', async () => {
    render(
      <MessageAttachments
        messageId={message.id}
        attachments={message.attachments}
      />,
    );

    const messageAttachmentsTitle = await screen.findByText('attachments');
    const messageAttachmentsCopyAll = await screen.findByLabelText(
      'conversation.copy.all.toworkspace',
    );
    const messageAttachmentsDownloadAll = await screen.findByLabelText(
      'download.all.attachment',
    );
    const messageAttachment1 = await screen.findByText(
      message.attachments[0].filename,
    );
    const messageAttachment2 = await screen.findByText(
      message.attachments[1].filename,
    );
    const messageAttachmentActionMoveTo = await screen.queryAllByLabelText(
      'conversation.copy.toworkspace',
    );
    const messageAttachmentActionDownload = await screen.queryAllByLabelText(
      'download.attachment',
    );

    expect(messageAttachmentsTitle).toBeInTheDocument();
    expect(messageAttachmentsCopyAll).toBeInTheDocument();
    expect(messageAttachmentsDownloadAll).toBeInTheDocument();
    expect(messageAttachment1).toBeInTheDocument();
    expect(messageAttachment2).toBeInTheDocument();
    expect(messageAttachmentActionMoveTo).toHaveLength(2);
    expect(messageAttachmentActionDownload).toHaveLength(2);
  });

  it('should not render attachements', async () => {
    render(<MessageAttachments messageId={message.id} attachments={[]} />);

    const messageAttachmentsTitle = await screen.queryByText('attachments');
    const messageAttachmentsCopyAll = await screen.queryByLabelText(
      'conversation.copy.all.toworkspace',
    );
    const messageAttachmentsDownloadAll = await screen.queryByLabelText(
      'download.all.attachment',
    );
    const messageAttachmentActionMoveTo = await screen.queryAllByLabelText(
      'conversation.copy.toworkspace',
    );
    const messageAttachmentActionDownload = await screen.queryAllByLabelText(
      'download.attachment',
    );

    expect(messageAttachmentsTitle).not.toBeInTheDocument();
    expect(messageAttachmentsCopyAll).not.toBeInTheDocument();
    expect(messageAttachmentsDownloadAll).not.toBeInTheDocument();
    expect(messageAttachmentActionMoveTo).toHaveLength(0);
    expect(messageAttachmentActionDownload).toHaveLength(0);
  });
});
