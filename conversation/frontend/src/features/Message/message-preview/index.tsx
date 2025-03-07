import { useDate } from '@edifice.io/react';
import { IconPaperclip, IconUndo } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { MessageMetadata } from '~/models';
import RecipientAvatar from './components/RecipientAvatar';
import { RecipientListPreview } from './components/RecipientListPreview';
import { SenderAvatar } from './components/SenderAvatar';
import { useMessageUserDisplayName } from './hooks/useUserDisplayName';

export interface MessagePreviewProps {
  message: MessageMetadata;
}

export function MessagePreview({ message }: MessagePreviewProps) {
  const { t } = useTranslation('conversation');
  const { folderId } = useParams<{ folderId: string }>();
  const { fromNow } = useDate();
  const senderDisplayName = useMessageUserDisplayName(message.from);

  return (
    <div className="d-flex flex-fill gap-12 align-items-center  overflow-hidden fs-6">
      {(message.response || message.forwarded) && (
        <IconUndo className="gray-800" title="message-response" />
      )}

      {'outbox' === folderId ? (
        <RecipientAvatar recipients={message.to} />
      ) : (
        <SenderAvatar authorId={message.from.id} />
      )}

      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          <div className="text-truncate flex-fill">
            {'outbox' === folderId ? (
              <RecipientListPreview message={message} />
            ) : (
              senderDisplayName
            )}
          </div>
          <div className="fw-bold text-nowrap fs-12 gray-800">
            {fromNow(message.date)}
          </div>
        </div>
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          {message.subject ? (
            <div className="text-truncate flex-fill">{message.subject}</div>
          ) : (
            <div className="text-truncate flex-fill text-gray-700">
              {t('nosubject')}
            </div>
          )}
          {message.hasAttachment && (
            <IconPaperclip
              className="gray-800"
              height={16}
              width={16}
              title="message-has-attachment"
            />
          )}
        </div>
      </div>
    </div>
  );
}
