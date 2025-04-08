import { IconButton, useDirectory } from '@edifice.io/react';
import { IconClose } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { useMessageUserDisplayName } from '~/hooks/useUserDisplayName';
import { Group, User } from '~/models';
import RecipientAvatar from './RecipientAvatar';

interface RecipientListSelectedItemProps {
  recipient: User | Group;
  type: 'user' | 'group';
  onRemoveClick: (recipient: User | Group) => void;
}

export function RecipientListSelectedItem({
  recipient,
  type,
  onRemoveClick,
}: RecipientListSelectedItemProps) {
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

  const handleRemoveKeyUp = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter') {
      onRemoveClick(recipient);
    }
  };

  const visibleType =
    'size' in recipient
      ? recipient.subType === 'BroadcastGroup'
        ? 'BroadcastGroup'
        : 'Group'
      : 'User';

  return (
    <div className="badge rounded-pill d-flex align-items-center gap-8 small fw-bold py-4 px-2 me-8 mt-4">
      <RecipientAvatar id={recipient.id} type={visibleType} size="xs" />
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
        onKeyUp={handleRemoveKeyUp}
      ></IconButton>
    </div>
  );
}
