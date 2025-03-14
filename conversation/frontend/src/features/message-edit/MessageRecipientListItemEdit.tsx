import { useDirectory } from '@edifice.io/react';
import { useMessageUserDisplayName } from '~/hooks/useUserDisplayName';
import { Group, User } from '~/models';

interface RecipientItemProps {
  recipient: User | Group;
  type: 'user' | 'group';
  hasLink?: boolean;
}

export function MessageRecipientListItemEdit({
  recipient,
  type,
  hasLink,
}: RecipientItemProps) {
  const recipientName = useMessageUserDisplayName(recipient);
  const { getUserbookURL } = useDirectory();
  return !hasLink ? (
    <span>{recipientName}</span>
  ) : (
    <a
      href={getUserbookURL(recipient.id, type)}
      target="_blank"
      rel="noopener noreferrer nofollow"
    >
      {recipientName}
    </a>
  );
}
