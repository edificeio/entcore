import { Avatar, AvatarSizes, useDirectory } from '@edifice.io/react';
import { IconBookmark, IconGroupAvatar } from '@edifice.io/react/icons';
import clsx from 'clsx';
import { useI18n } from '~/hooks';

interface RecipientAvatarProps {
  id: string;
  nbUsers?: number;
  className?: string;
  size?: AvatarSizes;
}

export function RecipientAvatar({
  id,
  nbUsers,
  className,
  size = 'sm',
}: RecipientAvatarProps) {
  const { t } = useI18n();
  const { getAvatarURL } = useDirectory();

  if (!nbUsers) {
    className = clsx(
      'bg-yellow-200 avatar rounded-circle',
      className,
      `avatar-${size}`,
    );
    return (
      <div
        className={className}
        aria-label={t('recipient.avatar.group')}
        role="img"
      >
        <IconBookmark className="w-16" />
      </div>
    );
  }
  if (nbUsers > 1) {
    className = clsx(
      'bg-secondary-200 avatar rounded-circle',
      className,
      `avatar-${size}`,
    );
    return (
      <div
        className={className}
        aria-label={t('recipient.avatar.group')}
        role="img"
      >
        <IconGroupAvatar className="w-16" />
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
