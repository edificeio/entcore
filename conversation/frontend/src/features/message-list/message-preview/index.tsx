import { useDate } from '@edifice.io/react';
import { IconPaperclip, IconUndo } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { MessageMetadata } from '~/models';
import { useMessageUserDisplayName } from '../../../hooks/useUserDisplayName';
import RecipientAvatar from './components/RecipientAvatar';
import { RecipientListPreview } from './components/RecipientListPreview';
import { SenderAvatar } from './components/SenderAvatar';

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

      {folderId && ['outbox', 'draft'].includes(folderId) ? (
        <RecipientAvatar recipients={message.to} />
      ) : (
        <SenderAvatar authorId={message.from.id} />
      )}

      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex flex-fill justify-content-between overflow-hidden gap-4">
          {folderId === 'draft' && (
            <span className="text-danger fw-bold">{t('draft')}</span>
          )}
          <div className="text-truncate flex-fill">
            {folderId === 'draft' && <RecipientListPreview message={message} />}
            {folderId === 'outbox' && (
              <RecipientListPreview message={message} hasPrefix />
            )}
            {folderId && ['inbox', 'trash'].includes(folderId) && (
              <span>{senderDisplayName}</span>
            )}
          </div>

          <div className="fw-bold text-nowrap fs-12 gray-800">
            <span>{fromNow(message.date)}</span>
          </div>
        </div>
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          {message.subject ? (
            <span className="text-truncate flex-fill">{message.subject}</span>
          ) : (
            <span className="text-truncate flex-fill text-gray-700">
              {t('nosubject')}
            </span>
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
