import { useDirectory } from '@edifice.io/react';
import { Fragment } from 'react/jsx-runtime';
import { Recipients } from '~/models';

export interface RecipientListProps {
  recipients: Recipients
}

export function MessageRecipientList({ recipients }: RecipientListProps) {
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
