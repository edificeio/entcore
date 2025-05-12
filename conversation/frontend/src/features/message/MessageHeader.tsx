import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { MessageRecipientList } from '../../components/MessageRecipientList';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageHeader({ message }: MessageHeaderProps) {
  const { t } = useI18n();
  const { fromNow } = useDate();
  const { getAvatarURL, getUserbookURL } = useDirectory();

  const { subject, from, date, to, cc } = message;
  const hasCC = cc.users.length > 0 || cc.groups.length > 0;

  return (
    <>
      {message && (
        <>
          <h4>{subject}</h4>
          <div className="d-flex align-items-center mt-16 gap-12 small">
            <Avatar
              alt={t('author.avatar')}
              size="sm"
              src={getAvatarURL(from.id, 'user')}
              variant="circle"
              className="align-self-start mt-4"
            />
            <div className="d-flex flex-fill flex-column overflow-hidden">
              <div className="d-flex flex-wrap column-gap-8">
                <a href={getUserbookURL(from.id, 'user')} className="fw-600">
                  {from.displayName}
                </a>
                <em className="text-gray-700">{fromNow(date)}</em>
              </div>
              <MessageRecipientList
                head={<b>{t('at')}</b>}
                recipients={to}
                hasLink
              />
              {hasCC && (
                <MessageRecipientList
                  head={<b>{t('cc')}</b>}
                  recipients={cc}
                  hasLink
                />
              )}
            </div>
          </div>
        </>
      )}
    </>
  );
}
