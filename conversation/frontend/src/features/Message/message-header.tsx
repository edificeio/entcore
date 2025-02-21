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

  const { subject, from, date, to, cc, cci } = message;
  const hasCC = cc.users.length > 0 || cc.groups.length > 0;
  const hasCCI = cci && (cci.users.length > 0 || cci.groups.length > 0);

  return (
    <>
      {message && (
        <>
          <h4>{subject}</h4>
          <div className="d-flex align-items-center mt-16 gap-12">
            <Avatar
              alt={t('author.avatar')}
              size="sm"
              src={getAvatarURL(from.id, 'user')}
              variant="circle"
              className='align-self-start mt-4'
            />
            <div className="d-flex flex-fill flex-column overflow-hidden">
              <div className="d-flex flex-wrap column-gap-8">
                <a
                  href={getUserbookURL(from.id, 'user')}
                  className="fw-600"
                >
                  {from.displayName}
                </a>
                <em className="text-gray-700">{fromNow(date)}</em>
              </div>
              <MessageRecipientList label={t("at")} recipients={to} />
              {hasCC && (
                <MessageRecipientList label={t("cc")} recipients={cc} />
              )}
              {hasCCI && (
                <MessageRecipientList label={t("cci")} recipients={cci} />
              )}
            </div>
          </div>
        </>
      )}
    </>
  );
}
