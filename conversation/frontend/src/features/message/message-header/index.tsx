import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { MessageRecipientList } from '~/components/MessageRecipientList/MessageRecipientList';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import './index.css';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageHeader({ message }: MessageHeaderProps) {
  const { t } = useI18n();
  const { fromNow } = useDate();
  const { getAvatarURL, getUserbookURL } = useDirectory();

  const { subject, from, date } = message;

  return (
    <header>
      {message && (
        <>
          <h4>
            {message.state === 'RECALL'
              ? t('conversation.recall.mail.subject.details')
              : subject}
          </h4>
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
                  className="fw-bold text-blue sender-link"
                >
                  {from.displayName}
                </a>
                {date && (
                  <span className="text-gray-700 fst-italic">
                    {fromNow(date)}
                  </span>
                )}
              </div>
              <MessageRecipientList message={message} />
            </div>
          </div>
        </>
      )}
    </header>
  );
}
