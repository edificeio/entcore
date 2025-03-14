import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { MessageAttachments } from '~/features/message/MessageAttachments';
import { Message as MessageData } from '~/models';

export function MessageBody({
  message,
  editMode,
}: {
  message: MessageData;
  editMode?: boolean;
}) {
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];
  const className = !editMode ? 'ps-md-48 my-md-24' : '';

  return (
    <section className={className}>
      <Editor
        id="messageBody"
        content={message.body}
        mode={editMode ? 'edit' : 'read'}
        variant="ghost"
        extensions={extensions}
      />
      <MessageAttachments
        attachments={message.attachments}
        messageId={message.id}
      />
    </section>
  );
}
