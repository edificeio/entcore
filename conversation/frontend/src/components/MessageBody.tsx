import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { MessageAttachments } from '~/components/message-attachments';
import { Message } from '~/models';
import { Suspense, useState } from 'react';
import { Alert, Button } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { EmptyScreen } from '@edifice.io/react';
import OriginalFormatModal from './OriginalFormatModal';
import illuRecall from '~/assets/illu-messageRecalled.svg';
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
  const { t } = useI18n();
  const content = message.body;
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];
  const [isOriginalFormatOpen, setOriginalFormatOpen] = useState(false);

  const handleContentChange = ({ editor }: { editor: any }) => {
    if (!editMode) return;
    onMessageChange?.({ ...message, body: editor?.getHTML() });
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
    <>
      {message.original_format_exists && !editMode && (
        <Alert
          type="warning"
          className="my-24"
          button={
            <Button
              color="tertiary"
              type="button"
              variant="ghost"
              className="text-gray-700"
              onClick={() => setOriginalFormatOpen(true)}
            >
              {t('message.warning.original.button')}
            </Button>
          }
        >
          {t('message.warning.original.text')}
        </Alert>
      )}
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

      <Suspense>
        {isOriginalFormatOpen && !editMode && (
          <OriginalFormatModal
            messageId={message.id}
            isOpen={isOriginalFormatOpen}
            onCancel={() => setOriginalFormatOpen(false)}
          />
        )}
      </Suspense>
    </>
  );
}
