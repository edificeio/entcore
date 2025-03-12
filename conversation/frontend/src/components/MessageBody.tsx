import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { MessageAttachments } from '~/components/MessageAttachments';
import { Message as MessageData } from '~/models';
import './MessageBody.css';

export interface MessageBodyProps {
  message: MessageData;
  editMode?: boolean;
}

export function MessageBody({ message, editMode }: MessageBodyProps) {
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];

  return (
    <>
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
        editMode={editMode}
      />
    </>
  );
}
