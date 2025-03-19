import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { MessageAttachments } from '~/components/message-attachments';
import { Message } from '~/models';
import './MessageBody.css';

export interface MessageBodyProps {
  message: Message;
  editMode?: boolean;
  onMessageChange?: (message: Message) => void;
}

export function MessageBody({
  message,
  editMode,
  onMessageChange,
}: MessageBodyProps) {
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];
  const content = message.body;

  const handleContentChange = ({ editor }: { editor: any }) => {
    if (!editMode) return;
    if (onMessageChange) {
      onMessageChange({ ...message, body: editor?.getHTML() });
    }
  };

  return (
    <section className="d-flex flex-column gap-16">
      <Editor
        id="messageBody"
        content={content}
        mode={editMode ? 'edit' : 'read'}
        variant="ghost"
        extensions={extensions}
        onContentChange={handleContentChange}
      />
      <MessageAttachments
        attachments={message.attachments}
        messageId={message.id}
        editMode={editMode}
      />
    </section>
  );
}
