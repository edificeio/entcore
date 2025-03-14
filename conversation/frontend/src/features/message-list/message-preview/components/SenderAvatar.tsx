import { Avatar, useDirectory } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';

interface SenderAvatarProps {
  authorId: string;
}

export function SenderAvatar({ authorId }: SenderAvatarProps) {
  const { t } = useTranslation('conversation');
  const { getAvatarURL } = useDirectory();
  return (
    <Avatar
      alt={t('author.avatar')}
      size="sm"
      src={getAvatarURL(authorId, 'user')}
      variant="circle"
    />
  );
}
