import { useDate, useDirectory } from '@edifice.io/react';
import { MessageRecipientList } from '~/components/MessageRecipientList/MessageRecipientList';
import { SenderAvatar } from '~/features/message-list/components/MessagePreview/components/SenderAvatar';
import { useI18n } from '~/hooks/useI18n';
import { Message } from '~/models';
import './MessageHeader.css';

export function MessageHeader({ message }: { message: Message }) {
  const { t } = useI18n();
  const { formatDate } = useDate();
  const { getUserbookURL } = useDirectory();

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
            <SenderAvatar
              authorId={message.from?.id}
              className="align-self-start mt-4"
            />
            <div className="d-flex flex-fill flex-column overflow-hidden">
              <div className="d-flex flex-wrap column-gap-8">
                {message.from?.id ? (
                  <a
                    href={getUserbookURL(message.from.id, 'user')}
                    className="fw-bold text-blue sender-link"
                  >
                    {message.from.displayName}
                  </a>
                ) : (
                  <span className="fw-bold text-blue">
                    {message.from?.displayName || ''}
                  </span>
                )}
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
