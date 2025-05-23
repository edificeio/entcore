import {
  Alert,
  Button,
  EmptyScreen,
  useEdificeClient,
} from '@edifice.io/react';
import {
  ConversationHistoryNodeView,
  ConversationHistoryRenderer,
  Editor,
  EditorRef,
} from '@edifice.io/react/editor';
import { Suspense, useRef, useState } from 'react';
import illuRecall from '~/assets/illu-messageRecalled.svg';
import { MessageAttachments } from '~/components/MessageAttachments/MessageAttachments';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import './MessageBody.css';
import OriginalFormatModal from './OriginalFormatModal';

export interface MessageBodyProps {
  message: Message;
  editMode?: boolean;
  isPrint?: boolean;
  onMessageChange?: (message: Message) => void;
}

export function MessageBody({
  message,
  editMode,
  isPrint,
  onMessageChange,
}: MessageBodyProps) {
  const { t } = useI18n();
  const { user } = useEdificeClient();
  const [content] = useState(message.body);
  const editorRef = useRef<EditorRef>(null);
  const extensions = [ConversationHistoryNodeView(ConversationHistoryRenderer)];
  const [isOriginalFormatOpen, setOriginalFormatOpen] = useState(false);

  const handleContentChange = ({ editor }: { editor: any }) => {
    if (!editMode) return;
    onMessageChange?.({ ...message, body: editor?.getHTML() });
  };

  return message.state === 'RECALL' && message.from.id !== user?.userId ? (
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
          ref={editorRef}
          id="messageBody"
          content={content}
          focus={'start'}
          mode={editMode ? 'edit' : 'read'}
          variant="ghost"
          extensions={extensions}
          onContentChange={handleContentChange}
        />
        <MessageAttachments message={message} editMode={editMode} />
      </section>

      {!isPrint && message.original_format_exists && !editMode && (
        <Alert
          type="warning"
          className="p-print-none my-24"
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
