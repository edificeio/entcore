import { Avatar, AvatarSizes, useDirectory } from '@edifice.io/react';
import {
  IconBookmark,
  IconGlobe2,
  IconGroupAvatar,
} from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks/useI18n';
import { VisibleType } from '~/models/visible';

interface RecipientAvatarProps {
  id: string;
  type: VisibleType;
  className?: string;
  size?: AvatarSizes;
}

export function RecipientAvatar({
  id,
  type,
  className,
  size = 'sm',
}: RecipientAvatarProps) {
  const { t } = useI18n();
  const { getAvatarURL } = useDirectory();

  const classNameGroup = clsx(
    'avatar rounded-circle',
    className,
    `avatar-${size}`,
    {
      'bg-secondary-200': type === 'Group' || type === 'BroadcastGroup',
      'bg-yellow-200': type === 'ShareBookmark',
    },
  );

  if (['ShareBookmark', 'BroadcastGroup', 'Group'].includes(type)) {
    return (
      <div
        className={classNameGroup}
        aria-label={t('recipient.avatar.group')}
        role="img"
      >
        {type === 'ShareBookmark' ? (
          <IconBookmark className="w-16" />
        ) : type === 'BroadcastGroup' ? (
          <IconGlobe2 className="w-16" />
        ) : (
          <IconGroupAvatar className="w-16" />
        )}
      </div>
    );
  }

  const avatarUrl = getAvatarURL(id, 'user');

  return (
    <Avatar
      alt={t('recipient.avatar')}
      size={size}
      src={avatarUrl}
      variant="circle"
      className={className}
    />
  );
}

export default RecipientAvatar;
