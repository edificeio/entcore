import { useDirectory, useEdificeClient } from '@edifice.io/react';
import clsx from 'clsx';
import { ReactNode } from 'react';
import { Fragment } from 'react/jsx-runtime';
import { useI18n } from '~/hooks';
import { Recipients } from '~/models';

export interface RecipientListProps {
  recipients: Recipients;
  head: ReactNode;
  color?: 'text-gray-800' | 'text-gray-700';
  truncate?: boolean;
  linkDisabled?: boolean;
}

export function MessageRecipientList({
  recipients,
  head,
  color = 'text-gray-700',
  truncate = false,
  linkDisabled = false,
}: RecipientListProps) {
  const recipientArray = [...recipients.users, ...recipients.groups];
  const { getUserbookURL } = useDirectory();
  const { user } = useEdificeClient();
  const { t } = useI18n();

  return (
    <div className={clsx({ 'text-truncate': truncate }, color)}>
      <span className="text-uppercase me-4">{head}</span>
      {recipientArray.map((recipient, index) => {
        const recipientText =
          user?.userId === recipient.id ? t('me') : recipient.displayName;
        let recipientElement;

        if (!linkDisabled) {
          const type = index < recipients.users.length ? 'user' : 'group';
          const url = getUserbookURL(recipient.id, type);
          recipientElement = (
            <a
              href={url}
              className={color}
              target="_blank"
              rel="noopener noreferrer nofollow"
            >
              {recipientText}
            </a>
          );
        } else {
          recipientElement = <span>{recipientText}</span>;
        }

        const isLast = index === recipientArray.length - 1;
        return (
          <Fragment key={recipient.id}>
            {recipientElement}
            {!isLast && ', '}
          </Fragment>
        );
      })}
    </div>
  );
}
