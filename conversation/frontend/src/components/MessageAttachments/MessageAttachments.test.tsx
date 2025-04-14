import { mockFullMessage } from '~/mocks';
import { render, screen } from '~/mocks/setup';
import { MessageAttachments } from './MessageAttachments';

const message = mockFullMessage;

describe('Message preview header component', () => {
  it('should render successfully', async () => {
    const { baseElement } = render(<MessageAttachments message={message} />);

    expect(baseElement).toBeTruthy();
  });

  it('should render multiple attachements successfully', async () => {
    render(<MessageAttachments message={message} editMode={true} />);

    const messageAttachmentsTitle = await screen.findByText('attachments');
    const messageAttachmentsCopyAll = await screen.findByTitle(
      'conversation.copy.all.toworkspace',
    );
    const messageAttachmentsDownloadAll = await screen.findByTitle(
      'download.all.attachment',
    );
    const messageAttachmentsRemoveAll = await screen.findByTitle(
      'remove.all.attachment',
    );
    const messageAttachment1 = await screen.findByText(
      message.attachments[0].filename,
    );
    const messageAttachment2 = await screen.findByText(
      message.attachments[1].filename,
    );
    const messageAttachmentActionMoveTo = await screen.queryAllByTitle(
      'conversation.copy.toworkspace',
    );
    const messageAttachmentActionDownload = await screen.queryAllByTitle(
      'download.attachment',
    );
    const messageAttachmentActionRemove =
      await screen.queryAllByTitle('remove.attachment');

    expect(messageAttachmentsTitle).toBeInTheDocument();
    expect(messageAttachmentsCopyAll).toBeInTheDocument();
    expect(messageAttachmentsDownloadAll).toBeInTheDocument();
    expect(messageAttachmentsRemoveAll).toBeInTheDocument();
    expect(messageAttachment1).toBeInTheDocument();
    expect(messageAttachment2).toBeInTheDocument();
    expect(messageAttachmentActionMoveTo).toHaveLength(2);
    expect(messageAttachmentActionDownload).toHaveLength(2);
    expect(messageAttachmentActionRemove).toHaveLength(2);
  });

  it('should not render attachements', async () => {
    const messageWoAttachments = { ...message, attachments: [] };
    render(<MessageAttachments message={messageWoAttachments} />);

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
