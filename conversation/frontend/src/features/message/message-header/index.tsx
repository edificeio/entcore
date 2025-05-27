import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { MessageRecipientList } from '~/components/MessageRecipientList/MessageRecipientList';
import { useI18n } from '~/hooks/useI18n';
import { Message } from '~/models';
import './index.css';

export function MessageHeader({ message }: { message: Message }) {
  const { t } = useI18n();
  const { formatDate } = useDate();
  const { getAvatarURL, getUserbookURL } = useDirectory();

  return (
    <header>
      {message && (
        <>
          <h4>
            {message.state === 'RECALL'
              ? t('conversation.recall.mail.subject.details')
              : message.subject || t('nosubject')}
          </h4>
          <div className="d-flex align-items-center mt-16 gap-12 small">
            <Avatar
              alt={t('author.avatar')}
              size="sm"
              src={getAvatarURL(message.from!.id, 'user')}
              variant="circle"
              className="align-self-start mt-4"
            />
            <div className="d-flex flex-fill flex-column overflow-hidden">
              <div className="d-flex flex-wrap column-gap-8">
                <a
                  href={getUserbookURL(message.from.id, 'user')}
                  className="fw-bold text-blue sender-link"
                >
                  {message.from.displayName}
                </a>
                {message.date && (
                  <span className="text-gray-700 fst-italic">
                    {formatDate(message.date, t('date.format.message'))}
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
