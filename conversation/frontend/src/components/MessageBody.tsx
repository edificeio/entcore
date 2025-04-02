import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { MessageAttachments } from '~/components/message-attachments';
import { Message } from '~/models';
import './MessageBody.css';

import illuRecall from '~/assets/illu-messageRecalled.svg';
import { useI18n } from '~/hooks';
import { EmptyScreen } from '@edifice.io/react';

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
  const { t } = useI18n();
  const content = message.body;
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];

  const handleContentChange = ({ editor }: { editor: any }) => {
    if (!editMode) return;
    if (onMessageChange) {
      onMessageChange({ ...message, body: editor?.getHTML() });
    }
  };

  return message.state === 'RECALL' ? (
    <div className="d-flex flex-column gap-16 align-items-center justify-content-center">
      <EmptyScreen
        imageSrc={illuRecall}
        imageAlt={t('conversation.recall.mail.subject')}
        title={t('conversation.recall.mail.subject')}
        text={t('conversation.recall.mail.content')}
      />
    </div>
  ) : (
    <section className="d-flex flex-column gap-16">
      <Editor
        id="messageBody"
        content={content}
        mode={editMode ? 'edit' : 'read'}
        variant="ghost"
        extensions={extensions}
        onContentChange={handleContentChange}
      />
      <MessageAttachments message={message} editMode={editMode} />
    </section>
  );
}
