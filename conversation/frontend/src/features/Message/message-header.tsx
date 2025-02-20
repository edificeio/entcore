import { Avatar, useDate, useDirectory } from '@edifice.io/react';
import { Fragment } from 'react/jsx-runtime';
import { useI18n } from '~/hooks';
import { Message, Recipients } from '~/models';

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
              <p className="text-gray-700">
                <strong className='text-uppercase r-4'>{t("at")} : </strong>
                <RecipientList recipients={message.to} />
              </p>
            </div>
          </div>
        </>
      )}
    </>
  );
}


function RecipientList({ recipients }: { recipients: Recipients }) {
  const recipientArray = [...recipients.users, ...recipients.groups]
  const { getUserbookURL } = useDirectory();

  return (
    <>
      {recipientArray.map((recipient, index) => {
        const link = <a
          href={getUserbookURL(recipient.id, 'user')}
          className="text-gray-700"
        >
          {recipient.displayName}
        </a>

        const isLast = index === recipientArray.length - 1;
        return (
          <Fragment key={recipient.id}>
            {link}
            {!isLast && ', '}
          </Fragment>
        );
      })}
    </>
  )

}
