import { Alert, Button, EmptyScreen } from '@edifice.io/react';
import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
} from '@edifice.io/react/editor';
import { Suspense, useEffect, useState } from 'react';
import illuRecall from '~/assets/illu-messageRecalled.svg';
import { MessageAttachments } from '~/components/MessageAttachments/MessageAttachments';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { useMessageUpdated } from '~/store';
import './MessageBody.css';
import OriginalFormatModal from './OriginalFormatModal';

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
  const [content, setContent] = useState('');
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];
  const [isOriginalFormatOpen, setOriginalFormatOpen] = useState(false);
  const messageUpdated = useMessageUpdated();

  const handleContentChange = ({ editor }: { editor: any }) => {
    if (!editMode) return;
    onMessageChange?.({ ...message, body: editor?.getHTML() });
  };
  useEffect(() => {
    // Set the content of the editor to the message body only on the first render
    setContent(message.body);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
          message={editMode && messageUpdated ? messageUpdated : message}
          editMode={editMode}
        />
      </section>

      {message.original_format_exists && !editMode && (
        <Alert
          type="warning"
          className="my-24"
          button={
            <Button
              type="button"
              color="tertiary"
              variant="ghost"
              className="btn-icon text-gray-700"
              onClick={() => setOriginalFormatOpen(true)}
            >
              {t('message.warning.original.button')}
            </Button>
          }
        >
          {t('message.warning.original.text')}
        </Alert>
      )}

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
