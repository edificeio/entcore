import { Dropdown } from '@edifice.io/react';
import { IconSuccessOutline } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks/useI18n';
import { useMessageUserDisplayName } from '~/hooks/useUserDisplayName';
import { Visible } from '~/models/visible';
import RecipientListAvatar from './RecipientListAvatar';
import { RecipientType } from './RecipientListEdit';
import './RecipientListItem.css';

interface MessageRecipientListItemProps {
  visible: Visible;
  onRecipientClick: (visible: Visible) => void;
  recipientType: RecipientType;
  disabled?: boolean;
  isSelected?: boolean;
}

export function RecipientListItem({
  visible,
  onRecipientClick,
  recipientType,
  disabled = false,
  isSelected: isSelectedList = false,
}: MessageRecipientListItemProps) {
  const { t, common_t } = useI18n();
  const recipientName = useMessageUserDisplayName(visible);

  const classNameProfile = clsx(
    `user-profile-${visible.profile?.toLowerCase()}`,
    {
      disabled,
    },
  );

  const className = clsx(
    'recipient-list-item d-flex flex-fill align-items-center gap-8',
    {
      disabled: disabled,
    },
  );
  const classNameTextDisabled = clsx({
    'text-gray-600': disabled,
  });

  const canBeUsedAsRecipient = visible.usedIn
    .map((ui) => ui.toLocaleLowerCase())
    .includes(recipientType);

  return (
    <Dropdown.Item
      type="select"
      onClick={() => onRecipientClick(visible)}
      disabled={disabled}
    >
      <div className={className}>
        <RecipientListAvatar id={visible.id} type={visible.type} />
        {(visible.type === 'Group' ||
          visible.type === 'ShareBookmark' ||
          visible.type === 'BroadcastGroup') && (
          <div className="d-flex flex-column small flex-fill">
            <strong className={classNameTextDisabled}>{recipientName}</strong>
            {(visible.nbUsers || !canBeUsedAsRecipient) && (
              <span className={'text-gray-700' + classNameTextDisabled}>
                {canBeUsedAsRecipient &&
                  visible.nbUsers &&
                  t('members', {
                    count: visible.nbUsers,
                  })}
                {canBeUsedAsRecipient &&
                  visible.structureName &&
                  ` - ${visible.structureName}`}
                {!canBeUsedAsRecipient &&
                  t('visible.usedIn.errorExplanation', {
                    recipientTypes: visible.usedIn.join(', '),
                  })}
              </span>
            )}
          </div>
        )}
        {visible.type === 'User' && (
          <div className="d-flex flex-column small flex-fill">
            <strong className={classNameTextDisabled}>
              {recipientName}
              {' - '}
              <span className={classNameProfile}>
                {common_t(visible.profile || '')}
              </span>
            </strong>
            {visible.profile === 'Student' && !!visible.relatives?.length && (
              <span className={'text-gray-700' + +classNameTextDisabled}>
                {t('visible.relatives')}
                {visible.relatives
                  .map((relative) => relative.displayName)
                  .join(', ')}
              </span>
            )}
            {visible.profile === 'Relative' && !!visible.children?.length && (
              <span className={'text-gray-700' + +classNameTextDisabled}>
                {t('visible.childrens')}
                {visible.children
                  .map((relative) => relative.displayName)
                  .join(', ')}
              </span>
            )}
          </div>
        )}
        {isSelectedList && <IconSuccessOutline className="text-gray-700" />}
      </div>
    </Dropdown.Item>
  );
}
