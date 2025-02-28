import { useDirectory, useEdificeClient } from '@edifice.io/react';
import clsx from 'clsx';
import { ReactElement } from 'react';
import { Fragment } from 'react/jsx-runtime';
import { useI18n } from '~/hooks';
import { Recipients } from '~/models';

export interface RecipientListProps {
  recipients: Recipients
  head: ReactElement | string
  color?: "text-gray-800" | "text-gray-700"
  truncate?: boolean
}

export function MessageRecipientList({
  recipients,
  head,
  color = "text-gray-700",
  truncate = false,
}: RecipientListProps) {
  const recipientArray = [...recipients.users, ...recipients.groups]
  const { getUserbookURL } = useDirectory();
  const { user } = useEdificeClient();
  const { t } = useI18n();


  return (
    <div className={clsx({ "text-truncate": truncate }, color)}>
      <span className='text-uppercase me-4'>{head}</span>
      {recipientArray.map((recipient, index) => {
        const type = index < recipients.users.length ? 'user' : 'group';
        const url = getUserbookURL(recipient.id, type)

        const link = <a
          href={url}
          className={color}
          target='_blank'
          rel="noopener noreferrer nofollow"
        >
          {user?.userId === recipient.id ? t('me') : recipient.displayName}
        </a>

        const isLast = index === recipientArray.length - 1;
        return (
          <Fragment key={recipient.id}>
            {link}
            {!isLast && ', '}
          </Fragment>
        );
      })}
    </div>
  )

}
