import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { Message } from '~/models';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageHeader({ message }: MessageHeaderProps) {
  const { t } = useI18n();

  const { getAvatarURL, getUserbookURL } = useDirectory();
  const { fromNow } = useDate();

  return (
    <>
      {message && (
        <>
          <h4>{message?.subject}</h4>
          <div className="d-flex align-items-center mt-16 gap-12">
            <Avatar
              alt={t('author.avatar')}
              size="sm"
              src={getAvatarURL(message.from.id, 'user')}
              variant="circle"
            />
            <div className="d-flex flex-fill flex-column align-items-start">
              <div className="d-flex gap-8">
                <a
                  href={getUserbookURL(message.from.id, 'user')}
                  className="fw-600"
                >
                  {message.from.displayName}
                </a>
                <div className="text-gray-700">
                  <em>{fromNow(message.date)}</em>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
