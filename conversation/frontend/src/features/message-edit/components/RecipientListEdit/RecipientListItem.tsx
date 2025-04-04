import { Dropdown } from '@edifice.io/react';
import { IconSuccessOutline } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';
import { useMessageUserDisplayName } from '~/hooks/useUserDisplayName';
import { Visible } from '~/models/visible';
import RecipientAvatar from './RecipientAvatar';
import './RecipientListItem.css';

interface MessageRecipientListItemProps {
  visible: Visible;
  onRecipientClick: (visible: Visible) => void;
  disabled?: boolean;
}

export function RecipientListItem({
  visible,
  onRecipientClick,
  disabled = false,
}: MessageRecipientListItemProps) {
  const { t, common_t } = useI18n();
  const recipientName = useMessageUserDisplayName(visible);

  const classNameProfile = clsx({
    'text-orange-300': visible.profile === 'Student' && disabled,
    'text-orange-500': visible.profile === 'Student' && !disabled,
    'text-blue-300': visible.profile === 'Relative' && disabled,
    'text-blue-500': visible.profile === 'Relative' && !disabled,
    'text-purple-300': visible.profile === 'Teacher' && disabled,
    'text-purple-500': visible.profile === 'Teacher' && !disabled,
    'text-green-300': visible.profile === 'Personnel' && disabled,
    'text-green-500': visible.profile === 'Personnel' && !disabled,
    'text-red-300':
      !['Student', 'Relative', 'Teacher', 'Personnel'].includes(
        visible.profile,
      ) && disabled,
    'text-red-500':
      !['Student', 'Relative', 'Teacher', 'Personnel'].includes(
        visible.profile,
      ) && !disabled,
  });

  const className = clsx(
    'recipient-list-item d-flex flex-fill align-items-center gap-8',
    {
      disabled: disabled,
    },
  );
  const classNameTextDisabled = clsx({
    'text-gray-600': disabled,
  });

  return (
    <Dropdown.Item
      type="select"
      onClick={() => onRecipientClick(visible)}
      disabled={disabled}
    >
      <div className={className}>
        <RecipientAvatar
          id={visible.id}
          nbUsers={visible.type === 'User' ? 1 : visible.nbUsers}
        />
        {visible.type === 'Group' && (
          <>
            {visible.nbUsers && visible.nbUsers > 1 && (
              <div className="d-flex flex-column small">
                <strong className={classNameTextDisabled}>
                  {recipientName}
                </strong>
                <span className={'text-gray-700' + classNameTextDisabled}>
                  {t('members', {
                    count: visible.nbUsers,
                  })}
                </span>
              </div>
            )}
          </>
        )}
        {visible.type === 'ShareBookmark' && (
          <div className="d-flex flex-column small flex-fill">
            <strong className={classNameTextDisabled}>{recipientName}</strong>
          </div>
        )}
        {visible.type === 'User' && (
          <div className="d-flex flex-column small flex-fill">
            <strong className={classNameTextDisabled}>
              {recipientName}
              {' - '}
              <span className={classNameProfile}>
                {common_t(visible.profile)}
              </span>
            </strong>
            {['Student', 'Relative'].includes(visible.profile) &&
              !!visible.relatives?.length && (
                <span className={'text-gray-700' + +classNameTextDisabled}>
                  {visible.profile === 'Student'
                    ? t('visible.relatives')
                    : t('visible.childrens')}
                  {visible.relatives
                    .map((relative) => relative.displayName)
                    .join(', ')}
                </span>
              )}
          </div>
        )}
        {disabled && <IconSuccessOutline className="text-gray-700" />}
      </div>
    </Dropdown.Item>
  );
}
