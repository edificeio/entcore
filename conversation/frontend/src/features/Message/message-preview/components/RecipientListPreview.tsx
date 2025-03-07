import { useTranslation } from 'react-i18next';
import { MessageMetadata } from '~/models';
import { MessageRecipientList } from '../../components/MessageRecipientList';

export interface RecipientListPreviewProps {
  message: MessageMetadata;
}

export function RecipientListPreview({ message }: RecipientListPreviewProps) {
  const { t } = useTranslation('conversation');
  const to = message.to;
  const cc = message.cc;
  const cci = message.cci ?? { users: [], groups: [] };
  const recipients = {
    users: [...to.users, ...cc.users, ...cci?.users],
    groups: [...to.groups, ...cc.groups, ...cci?.groups],
  };
  return (
    <MessageRecipientList
      head={t('at')}
      recipients={recipients}
      color="text-gray-800"
      truncate
    />
  );
}
