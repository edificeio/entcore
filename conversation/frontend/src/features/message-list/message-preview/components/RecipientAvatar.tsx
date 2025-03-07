import { Avatar } from '@edifice.io/react';
import { IconGroupAvatar } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { Recipients } from '~/models';
import { useRecipientAvatar } from '../../../../hooks/useRecipientAvatar';

interface RecipientAvatarProps {
  recipients: Recipients;
}

export function RecipientAvatar({ recipients }: RecipientAvatarProps) {
  const { t } = useTranslation('conversation');
  const { recipientCount, url } = useRecipientAvatar(recipients);

  if (recipientCount > 1) {
    return (
      <div
        className="bg-orange-200 avatar avatar-sm rounded-circle"
        aria-label={t('recipient.avatar.group')}
        role="img"
      >
        <IconGroupAvatar className="w-16" />
      </div>
    );
  } else if (recipientCount === 1 && url) {
    return (
      <Avatar
        alt={t('recipient.avatar')}
        size="sm"
        src={url}
        variant="circle"
      />
    );
  } else {
    return null;
  }
}

export default RecipientAvatar;
