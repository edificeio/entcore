import {
  IconDepositeInbox,
  IconSend,
  IconWrite,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useMessageOriginFolder } from '~/hooks/useMessageOriginFolder';
import { MessageMetadata } from '~/models';

type UserFolderIconProps = {
  message: MessageMetadata;
};

export function UserFolderIcon({ message }: UserFolderIconProps) {
  const { t } = useTranslation('conversation');
  const originFolderId = useMessageOriginFolder(message);

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
