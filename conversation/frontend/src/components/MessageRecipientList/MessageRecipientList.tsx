import { useTranslation } from 'react-i18next';
import { MessageRecipientSubList } from '~/components/MessageRecipientList/components/MessageRecipientSubList';
import useMessageCciToDisplay from '~/components/MessageRecipientList/hooks/useMessageCciToDisplay';
import { MessageBase } from '~/models';

export interface MessageRecipientListProps {
  message: MessageBase;
  color?: 'text-gray-800' | 'text-gray-700';
  hasLink?: boolean;
  inline?: boolean;
}

export function MessageRecipientList({
  message,
  inline,
}: MessageRecipientListProps) {
  const { t } = useTranslation('conversation');

  const { to, cc } = message;
  const hasTo = to.users.length > 0 || to.groups.length > 0;
  const hasCC = cc.users.length > 0 || cc.groups.length > 0;
  const cciToDisplay = useMessageCciToDisplay(message);

  return (
    <>
      {hasTo && (
        <MessageRecipientSubList
          head={t('at')}
          recipients={to}
          inline={inline}
        />
      )}
      {hasCC && (
        <MessageRecipientSubList
          head={t('cc')}
          recipients={cc}
          inline={inline}
        />
      )}
      {cciToDisplay && (
        <MessageRecipientSubList
          head={t('cci')}
          recipients={cciToDisplay}
          inline={inline}
        />
      )}
    </>
  );
}
