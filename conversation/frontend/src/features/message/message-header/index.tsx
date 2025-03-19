import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import useMessageCciToDisplay from '~/features/message/message-header/useMessageCciToDisplay';
import { Message } from '~/models';
import { MessageRecipientList } from '../../../components/MessageRecipientList';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageHeader({ message }: MessageHeaderProps) {
  const { t } = useI18n();
  const { fromNow } = useDate();
  const { getAvatarURL, getUserbookURL } = useDirectory();

  const { subject, from, date, to, cc } = message;
  const hasTo = to.users.length > 0 || to.groups.length > 0;
  const hasCC = cc.users.length > 0 || cc.groups.length > 0;
  const cciToDisplay = useMessageCciToDisplay(message);

  return (
    <header>
      {message && (
        <>
          <h4>{subject}</h4>
          <div className="d-flex align-items-center mt-16 gap-12 small">
            <Avatar
              alt={t('author.avatar')}
              size="sm"
              src={getAvatarURL(from!.id, 'user')}
              variant="circle"
              className="align-self-start mt-4"
            />
            <div className="d-flex flex-fill flex-column overflow-hidden">
              <div className="d-flex flex-wrap column-gap-8">
                <a
                  href={getUserbookURL(from.id, 'user')}
                  className="fw-bold text-blue"
                >
                  {from.displayName}
                </a>
                {date && (
                  <span className="text-gray-700 fst-italic">
                    {fromNow(date)}
                  </span>
                )}
              </div>
              {hasTo && (
                <MessageRecipientList
                  head={<b>{t('at')}</b>}
                  recipients={to}
                  hasLink
                />
              )}
              {hasCC && (
                <MessageRecipientList
                  head={<b>{t('cc')}</b>}
                  recipients={cc}
                  hasLink
                />
              )}
              {cciToDisplay && (
                <MessageRecipientList
                  head={<b>{t('cci')}</b>}
                  recipients={cciToDisplay}
                  hasLink
                />
              )}
            </div>
          </div>
        </>
      )}
    </header>
  );
}
