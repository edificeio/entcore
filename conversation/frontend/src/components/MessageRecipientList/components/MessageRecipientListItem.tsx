import { useDirectory } from '@edifice.io/react';
import { useMessageUserDisplayName } from '~/hooks/useUserDisplayName';
import { Group, User } from '~/models';
import './MessageRecipientListItem.css';

interface RecipientItemProps {
  recipient: User | Group;
  color: string;
  type: 'user' | 'group';
  hasLink?: boolean;
}

export function MessageRecipientListItem({
  recipient,
  color,
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
      className={`recipient-link ${color}`}
      target="_blank"
      rel="noopener noreferrer nofollow"
    >
      {recipientName}
    </a>
  );
}
