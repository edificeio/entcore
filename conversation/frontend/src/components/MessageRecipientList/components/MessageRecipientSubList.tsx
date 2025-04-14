import clsx from 'clsx';
import { useTranslation } from 'react-i18next';
import { Recipients } from '~/models';
import { MessageRecipientListItem } from './MessageRecipientListItem';

export interface MessageRecipientSubListProps {
  recipients: Recipients;
  head: string;
  truncate?: boolean;
  inline?: boolean;
}

export function MessageRecipientSubList({
  recipients,
  head,
  inline = false,
}: MessageRecipientSubListProps) {
  const recipientArray = [...recipients.users, ...recipients.groups];
  const { t } = useTranslation('conversation');

  const hasLink = !inline;
  const color = inline ? 'text-gray-800' : 'text-gray-700';
  const headIsBold = !inline;

  return (
    <div className={clsx({ 'd-inline pe-4': inline }, color)}>
      <span className={clsx('me-4', { 'fw-bold': headIsBold })}>{head}</span>
      <ul
        className={'list-unstyled mb-0 d-inline'}
        aria-label={t('recipient.list')}
      >
        {recipientArray.map((recipient, index) => {
          const type = index < recipients.users.length ? 'user' : 'group';
          const isLast = index === recipientArray.length - 1;
          return (
            <li key={head + recipient.id} className="d-inline">
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
