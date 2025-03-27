import {
  IconDepositeInbox,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';

type UserFolderIconProps = {
  originFolderId: string;
};

export function UserFolderIcon({ originFolderId }: UserFolderIconProps) {
  const { t } = useTranslation('conversation');

  const iconProps = {
    'className': 'gray-800',
    'width': 16,
    'height': 16,
    'aria-hidden': false,
    'role': 'img',
  };

  return (
    <>
      {originFolderId === 'draft' && (
        <IconWrite title={t('mail-new')} {...iconProps} />
      )}

      {originFolderId === 'inbox' && (
        <IconDepositeInbox title={t('mail-in')} {...iconProps} />
      )}

      {originFolderId === 'outbox' && (
        <IconSend title={t('mail-out')} {...iconProps} />
      )}
    </>
  );
}
