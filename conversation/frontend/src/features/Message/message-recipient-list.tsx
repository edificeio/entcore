import { useDirectory, useEdificeClient } from '@edifice.io/react';
import { Fragment } from 'react/jsx-runtime';
import { useI18n } from '~/hooks';
import { Recipients } from '~/models';

export interface RecipientListProps {
  recipients: Recipients
  label: string
}

export function MessageRecipientList({
  recipients,
  label
}: RecipientListProps) {
  const recipientArray = [...recipients.users, ...recipients.groups]
  const { getUserbookURL } = useDirectory();
  const { user } = useEdificeClient();
  const { t } = useI18n();

  return (
    <div className="text-gray-700 text-truncate">
      <strong className='text-uppercase'>{label} : </strong>
      {recipientArray.map((recipient, index) => {
        const link = <a
          href={getUserbookURL(recipient.id, 'user')}
          className="text-gray-700"
          target='_blank'
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
