import clsx from 'clsx';
import { Fragment, ReactNode } from 'react';
import { Recipients } from '~/models';
import { MessageRecipientListItem } from './MessageRecipientListItem';

export interface RecipientListProps {
  recipients: Recipients;
  head?: ReactNode;
  color?: 'text-gray-800' | 'text-gray-700';
  truncate?: boolean;
  hasLink?: boolean;
}

export function MessageRecipientList({
  recipients,
  head,
  color = 'text-gray-700',
  truncate = false,
  hasLink = false,
}: RecipientListProps) {
  const recipientArray = [...recipients.users, ...recipients.groups];

  return (
    <div className={clsx({ 'text-truncate': truncate }, color)}>
      {head && <span className="text-uppercase me-4">{head}</span>}
      <ul className={'list-unstyled mb-0 d-inline'}>
        {recipientArray.map((recipient, index) => {
          const type = index < recipients.users.length ? 'user' : 'group';
          const isLast = index === recipientArray.length - 1;
          return (
            <li key={recipient.id} className="d-inline">
              <MessageRecipientListItem
                recipient={recipient}
                color={color}
                type={type}
                hasLink={hasLink}
              />
              {!isLast && ', '}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
