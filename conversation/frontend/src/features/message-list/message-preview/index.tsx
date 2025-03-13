import { useDate } from '@edifice.io/react';
import { IconPaperclip, IconUndo } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useSelectedFolder } from '~/hooks';
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
  const { folderId } = useSelectedFolder();
  const { fromNow } = useDate();
  const senderDisplayName = useMessageUserDisplayName(message.from);

  console.log('message:', message);
  return (
    <div className="d-flex flex-fill gap-12 align-items-center  overflow-hidden fs-6">
      {message.response && (
        <IconUndo
          aria-hidden={false}
          className="gray-800"
          height={16}
          role="img"
          title={t('message.replied')}
          width={16}
        />
      )}

      {folderId === 'outbox' || folderId === 'draft' ? (
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
              <RecipientListPreview message={message} head={t('at')} />
            )}
            {(folderId === 'inbox' || folderId === 'trash') && (
              <span>{senderDisplayName}</span>
            )}
          </div>

          <span className="text-nowrap caption fw-bold gray-800">
            {fromNow(message.date)}
          </span>
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
              aria-hidden={false}
              className="gray-800"
              height={16}
              role="img"
              title={t('message.has.attachment')}
              width={16}
            />
          )}
        </div>
      </div>
    </div>
  );
}
