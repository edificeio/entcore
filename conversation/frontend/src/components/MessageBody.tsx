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
import { ConversationHistory } from '@edifice.io/tiptap-extensions/conversation-history';
import { ConversationHistoryBody } from '@edifice.io/tiptap-extensions/conversation-history-body';
import { Suspense, useEffect, useLayoutEffect, useRef, useState } from 'react';
import illuRecall from '~/assets/illu-messageRecalled.svg';
import { MessageAttachments } from '~/components/MessageAttachments/MessageAttachments';
import { useI18n } from '~/hooks/useI18n';
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
  const { user } = useEdificeClient();
  const [content, setContent] = useState('');
  const editorRef = useRef<EditorRef>(null);
  const extensions = [
    ConversationHistoryBody,
    ConversationHistory,
    ConversationHistoryNodeView(ConversationHistoryRenderer),
  ];
  const [isOriginalFormatOpen, setOriginalFormatOpen] = useState(false);
  const messageUpdated = useMessageUpdated();

  const handleContentChange = ({ editor }: { editor: any }) => {
    if (!editMode) return;
    onMessageChange?.({ ...message, body: editor?.getHTML() });
  };
  useLayoutEffect(() => {
    // Set the content of the editor to the message body only on the first render
    setContent(message.body);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    // Force the editor to set the focus on the start of the content
    editorRef.current?.setFocus('start');
  }, [content]);

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
          mode={editMode ? 'edit' : 'read'}
          focus={'start'}
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
