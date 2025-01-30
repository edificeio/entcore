import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { IconPaperclip, IconUndo } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { MessageMetadata } from '~/models';

export interface MessagePreviewProps {
  message: MessageMetadata;
}

export function MessagePreview({ message }: MessagePreviewProps) {
  const { t } = useTranslation('conversation');

  const { getAvatarURL } = useDirectory();
  const { fromNow } = useDate();

  return (
    <div className="d-flex gap-12 align-items-center flex-fill overflow-hidden fs-6">
      {(message.response || message.forwarded) && (
        <IconUndo className="gray-800" />
      )}
      <Avatar
        alt={t('author.avatar')}
        size="sm"
        src={getAvatarURL(message.from.id, 'user')}
        variant="circle"
      />
      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          <div className="text-truncate flex-fill">
            {message.from.displayName}
          </div>
          <div className="fw-bold text-nowrap fs-12 gray-800">
            {fromNow(message.date)}
          </div>
        </div>
        <div className="d-flex flex-fill justify-content-between overflow-hidden">
          {message.subject ? (
            <div className="text-truncate flex-fill">{message.subject}</div>
          ) : (
            <div className="text-truncate flex-fill gray-800">
              {t('nosubject')}
            </div>
          )}
          {!message.hasAttachments && (
            <IconPaperclip className="gray-800" height={16} width={16} />
          )}
        </div>
      </div>
    </div>
  );
}
