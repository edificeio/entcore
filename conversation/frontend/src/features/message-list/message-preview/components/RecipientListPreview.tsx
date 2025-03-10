import { useTranslation } from 'react-i18next';
import { MessageRecipientList } from '~/components/MessageRecipientList';
import { MessageMetadata } from '~/models';

export interface RecipientListPreviewProps {
  message: MessageMetadata;
  hasPrefix?: boolean;
}

export function RecipientListPreview({
  message,
  hasPrefix,
}: RecipientListPreviewProps) {
  const { t } = useTranslation('conversation');
  const to = message.to;
  const cc = message.cc;
  const cci = message.cci ?? { users: [], groups: [] };
  const recipients = {
    users: [...to.users, ...cc.users, ...cci.users],
    groups: [...to.groups, ...cc.groups, ...cci.groups],
  };
  return (
    <MessageRecipientList
      head={hasPrefix ? t('at') : null}
      recipients={recipients}
      color="text-gray-800"
      truncate
    />
  );
}
