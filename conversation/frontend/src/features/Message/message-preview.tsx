import { Avatar, useDate, useDirectory } from '@edifice.io/react';
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
    <div className="d-flex gap-12 align-items-center flex-fill overflow-hidden fs-14">
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
        {message.subject && (
          <div className="text-truncate flex-fill">{message.subject}</div>
        )}
        {!message.subject && (
          <div className="text-truncate flex-fill gray-800">
            {t('nosubject')}
          </div>
        )}
      </div>
    </div>
  );
}
