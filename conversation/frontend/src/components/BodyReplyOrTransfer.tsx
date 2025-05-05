import { useDate } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { Message, Recipients } from '~/models';

export function BodyReplyOrTransfer(
  messageOrigin: Message,
  isTranferAction: boolean,
) {
  const { t } = useI18n();
  const { formatDate } = useDate();
  const displayRecipient = (recipients: Recipients) => {
    const usersDisplayName = recipients.users
      .map((user) => user.displayName)
      .join(', ');
    const groupsDisplayName = recipients.groups
      .map((group) => group.displayName)
      .join(', ');
    return (
      usersDisplayName +
      (recipients.users.length > 0 && recipients.groups.length > 0
        ? ', '
        : '') +
      groupsDisplayName
    );
  };

  return `<p></p><p></p>
          <div class="${isTranferAction ? '' : 'conversation-history'}">
              ${isTranferAction ? '<p><span style="font-size: 14px; font-weight:400;">' + t('transfer.title') + '</span></p>' : ''}
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.from') + messageOrigin.from?.displayName}</span></p>
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.date') + (!messageOrigin.date ? '' : formatDate(messageOrigin.date, 'long'))}</span></p>
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.subject') + messageOrigin.subject}</span></p>
              <p><span style="font-size: 14px; font-weight:400;">${t('transfer.to') + displayRecipient(messageOrigin.to)}</span></p>
              ${messageOrigin.cc.users.length || messageOrigin.cc.groups.length ? '<p><span style="font-size: 14px; font-weight:400;">' + t('transfer.cc') + displayRecipient(messageOrigin.cc) + '</span></p>' : ''}
            <p class="${isTranferAction ? '' : 'conversation-history-body'}">
              ${messageOrigin.body}
            </p>
          </div>`;
}
