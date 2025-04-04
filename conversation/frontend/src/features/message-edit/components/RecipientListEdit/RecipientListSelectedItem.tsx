import { IconButton, useDirectory } from '@edifice.io/react';
import { IconClose } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { useMessageUserDisplayName } from '~/hooks/useUserDisplayName';
import { Group, User } from '~/models';
import RecipientAvatar from './RecipientAvatar';

interface MessageRecipientListSelectedItemProps {
  recipient: User | Group;
  type: 'user' | 'group';
  onRemoveClick: (recipient: User | Group) => void;
}

export function MessageRecipientListSelectedItem({
  recipient,
  type,
  onRemoveClick,
}: MessageRecipientListSelectedItemProps) {
  const { common_t } = useI18n();
  const recipientName = useMessageUserDisplayName(recipient);
  const { getUserbookURL } = useDirectory();
  const url = getUserbookURL(recipient.id, type);

  const classNameProfile =
    type === 'user'
      ? clsx({
          'text-orange-500': (recipient as User).profile === 'Student',
          'text-blue-500': (recipient as User).profile === 'Relative',
          'text-purple-500': (recipient as User).profile === 'Teacher',
          'text-green-500': (recipient as User).profile === 'Personnel',
          'text-red-500': ![
            'Student',
            'Relative',
            'Teacher',
            'Personnel',
          ].includes((recipient as User).profile),
        })
      : '';

  return (
    <div className="badge rounded-pill d-flex align-items-center gap-8 small fw-bold p-4">
      <RecipientAvatar
        id={recipient.id}
        nbUsers={type === 'user' ? 1 : (recipient as Group).size}
        size="xs"
      />
      {!url ? (
        <span>{recipientName}</span>
      ) : (
        <a
          href={getUserbookURL(recipient.id, type)}
          target="_blank"
          rel="noopener noreferrer"
          className="text-gray-800"
        >
          {recipientName}
          {type === 'user' && (
            <>
              {' - '}
              <span className={classNameProfile}>
                {common_t((recipient as User).profile)}
              </span>
            </>
          )}
        </a>
      )}
      <IconButton
        icon={<IconClose />}
        variant="ghost"
        color="tertiary"
        className="rounded-pill p-4"
        onClick={() => {
          onRemoveClick(recipient);
        }}
      ></IconButton>
    </div>
  );
}
