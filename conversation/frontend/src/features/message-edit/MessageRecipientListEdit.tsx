import { Fragment, ReactNode } from 'react';
import { Recipients } from '~/models';
import { MessageRecipientListItemEdit } from './MessageRecipientListItemEdit';

export interface RecipientListProps {
  recipients: Recipients;
  head: ReactNode;
  hasLink?: boolean;
}

export function MessageRecipientListEdit({
  recipients,
  head,
  hasLink = false,
}: RecipientListProps) {
  const recipientArray = [...recipients.users, ...recipients.groups];

  return (
    <div className="d-flex flex-column p-16">
      <div className="d-flex align-items-center">
        <span className="text-capitalize me-4">{head}</span>
      </div>
      <div>
        {recipientArray.map((recipient, index) => {
          const type = index < recipients.users.length ? 'user' : 'group';
          const isLast = index === recipientArray.length - 1;
          return (
            <Fragment key={recipient.id}>
              <MessageRecipientListItemEdit
                recipient={recipient}
                type={type}
                hasLink={hasLink}
              />
              {!isLast && ', '}
            </Fragment>
          );
        })}
      </div>
    </div>
  );
}
