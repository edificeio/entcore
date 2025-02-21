import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { MessageRecipientList } from './message-recipient-list';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageHeader({ message }: MessageHeaderProps) {
  const { t } = useI18n();
  const { fromNow } = useDate();
  const { getAvatarURL, getUserbookURL } = useDirectory();

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
              className='align-self-start mt-4'
            />
            <div className="d-flex flex-fill flex-column overflow-hidden">
              <div className="d-flex flex-wrap column-gap-8">
                <a
                  href={getUserbookURL(message.from.id, 'user')}
                  className="fw-600"
                >
                  {message.from.displayName}
                </a>
                <em className="text-gray-700">{fromNow(message.date)}</em>
              </div>
              <div className="text-gray-700 text-truncate">
                <strong className='text-uppercase'>{t("at")} : </strong>
                <MessageRecipientList recipients={message.to} />
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
