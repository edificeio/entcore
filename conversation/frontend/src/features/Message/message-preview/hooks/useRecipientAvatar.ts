import { useDirectory } from '@edifice.io/react';
import { Recipients } from '~/models';

export function useRecipientAvatar(recipients: Recipients) {
  const { getAvatarURL } = useDirectory();
  const recipientCount = recipients.users.length + recipients.groups.length;

  if (recipientCount > 1) {
    return { recipientCount, url: null };
  } else {
    const firstRecipient = recipients.users[0] || recipients.groups[0];
    const firstRecipientType = recipients.users.length > 0 ? 'user' : 'group';
    const url = getAvatarURL(firstRecipient.id, firstRecipientType);
    return { recipientCount, url };
  }
}
